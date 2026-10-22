package com.licensegraph.lineage;

import com.licensegraph.asset.AssetLookup;
import com.licensegraph.asset.DerivationConflictChecker;
import com.licensegraph.asset.PolicyConflict;
import com.licensegraph.common.AssetRef;
import com.licensegraph.common.DerivationInput;
import com.licensegraph.common.LineageEdge;
import com.licensegraph.common.LineageGraph;
import com.licensegraph.common.exception.BadRequestException;
import com.licensegraph.common.exception.ConflictException;
import com.licensegraph.common.exception.ErrorCode;
import com.licensegraph.common.exception.UnprocessableEntityException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Records derivations: validates every referenced asset, rejects cycles and duplicate edges,
 * stores the edges with any upstream policy conflicts, and returns the result (C2 to C5).
 *
 * <p>All checks and inserts for one request run in a single transaction while holding a
 * per-client lock, so two concurrent requests cannot jointly form a cycle. Any failure rolls
 * back the whole batch.
 */
// AI-assisted: drafted with Claude, reviewed by Srishti Singh.
@Service
public class DerivationServiceImpl implements DerivationService {

    private final DerivationEdgeRepository edges;
    private final AssetLookup assets;
    private final DerivationConflictChecker conflictChecker;
    private final LineageQueryService lineage;
    private final TransactionTemplate transaction;

    /** One lock object per client. Same client waits; different clients never block each other. */
    private final ConcurrentMap<String, Object> clientLocks = new ConcurrentHashMap<>();

    public DerivationServiceImpl(DerivationEdgeRepository edges,
                                 AssetLookup assets,
                                 DerivationConflictChecker conflictChecker,
                                 LineageQueryService lineage,
                                 PlatformTransactionManager transactionManager) {
        this.edges = edges;
        this.assets = assets;
        this.conflictChecker = conflictChecker;
        this.lineage = lineage;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public DerivationResult recordDerivation(String clientId, AssetRef output, List<DerivationInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw new BadRequestException(ErrorCode.VALIDATION_ERROR, "At least one input is required");
        }
        // The lock is held until the transaction has committed, so the next request sees our edges.
        Object clientLock = clientLocks.computeIfAbsent(clientId, id -> new Object());
        synchronized (clientLock) {
            return transaction.execute(status -> recordInTransaction(clientId, output, inputs));
        }
    }

    private DerivationResult recordInTransaction(String clientId, AssetRef output, List<DerivationInput> inputs) {
        // Step 2: every referenced asset must exist for this client (C2).
        requireKnown(clientId, output);
        for (DerivationInput input : inputs) {
            requireKnown(clientId, input.asset());
        }

        // Step 3: no input may already descend from the output (C3).
        for (DerivationInput input : inputs) {
            Optional<List<AssetRef>> cycle = findCyclePath(clientId, input.asset(), output);
            if (cycle.isPresent()) {
                throw ConflictException.cycleDetected(cycle.get());
            }
        }

        // Step 4: no exact duplicate, stored or within this request (C4).
        rejectDuplicates(clientId, output, inputs);

        // Step 5: upstream graph per input, then ask Module B for conflicts (C5).
        Map<AssetRef, LineageGraph> upstreamByInput = new LinkedHashMap<>();
        for (DerivationInput input : inputs) {
            upstreamByInput.put(input.asset(), lineage.findAncestors(clientId, input.asset()));
        }
        List<PolicyConflict> conflicts = conflictChecker.check(clientId, inputs, upstreamByInput);

        // Step 6: build each edge, attach the conflicts from its own input's upstream, save all.
        Instant now = Instant.now();
        List<DerivationEdge> toSave = new ArrayList<>();
        for (DerivationInput input : inputs) {
            DerivationEdge edge = new DerivationEdge(clientId, input.asset(), output, input.relation(), now);
            Set<AssetRef> upstream = upstreamByInput.get(input.asset()).nodes();
            edge.recordConflicts(conflicts.stream().filter(c -> upstream.contains(c.source())).toList());
            toSave.add(edge);
        }
        List<LineageEdge> created = edges.saveAll(toSave).stream()
                .map(DerivationEdge::toLineageEdge)
                .toList();

        // Step 7: the output's ancestors are exactly the union of its inputs' upstream graphs.
        Set<AssetRef> ancestors = new HashSet<>();
        for (LineageGraph graph : upstreamByInput.values()) {
            ancestors.addAll(graph.nodes());
        }
        return new DerivationResult(created, conflicts, ancestors.size());
    }

    /** 422 UNKNOWN_INPUT_ASSET when the asset is missing or belongs to another client. */
    private void requireKnown(String clientId, AssetRef ref) {
        if (assets.findAsset(clientId, ref).isEmpty()) {
            throw new UnprocessableEntityException(
                    ErrorCode.UNKNOWN_INPUT_ASSET, "Unknown asset in request: " + ref);
        }
    }

    /**
     * Walks up from the input. If the output is reached, adding input -> output would close a
     * loop. Returns the path [output, input, ..., output], matching the plan's cyclePath format.
     */
    private Optional<List<AssetRef>> findCyclePath(String clientId, AssetRef input, AssetRef output) {
        if (input.equals(output)) {
            return Optional.of(List.of(output, input));
        }
        Map<AssetRef, AssetRef> reachedFrom = new HashMap<>();
        Set<AssetRef> visited = new HashSet<>();
        Deque<AssetRef> queue = new ArrayDeque<>();
        visited.add(input);
        queue.add(input);

        while (!queue.isEmpty()) {
            AssetRef current = queue.poll();
            for (DerivationEdge edge : edges.findByClientIdAndOutputAssetIdAndOutputVersion(
                    clientId, current.assetId(), current.version())) {
                AssetRef parent = edge.input();
                if (!visited.add(parent)) {
                    continue;
                }
                reachedFrom.put(parent, current);
                if (parent.equals(output)) {
                    return Optional.of(buildPath(output, parent, reachedFrom));
                }
                queue.add(parent);
            }
        }
        return Optional.empty();
    }

    /** Follows the reachedFrom links back to the input, then puts the output in front. */
    private List<AssetRef> buildPath(AssetRef output, AssetRef found, Map<AssetRef, AssetRef> reachedFrom) {
        LinkedList<AssetRef> path = new LinkedList<>();
        for (AssetRef node = found; node != null; node = reachedFrom.get(node)) {
            path.addFirst(node);
        }
        path.addFirst(output);
        return path;
    }

    /** 409 DUPLICATE_EDGE if an identical edge is already stored or repeated in this request. */
    private void rejectDuplicates(String clientId, AssetRef output, List<DerivationInput> inputs) {
        Set<String> seenInRequest = new HashSet<>();
        for (DerivationInput input : inputs) {
            AssetRef in = input.asset();
            boolean repeated = !seenInRequest.add(in + "|" + input.relation());
            boolean stored = edges.existsByClientIdAndInputAssetIdAndInputVersionAndOutputAssetIdAndOutputVersionAndRelation(
                    clientId, in.assetId(), in.version(), output.assetId(), output.version(), input.relation());
            if (repeated || stored) {
                throw ConflictException.duplicateEdge();
            }
        }
    }
}