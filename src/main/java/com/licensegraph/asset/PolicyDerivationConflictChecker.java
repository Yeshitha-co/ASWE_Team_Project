package com.licensegraph.asset;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.DerivationInput;
import com.licensegraph.common.LineageGraph;
import com.licensegraph.common.RelationType;
import com.licensegraph.policy.PolicyLookup;
import com.licensegraph.policy.PolicyView;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI-assisted: Finds upstream policy conflicts for a new derivation (Module B, task B7).
 *
 * <p>Module C passes each input's upstream graph: the input itself plus all of its ancestors.
 * For every node in those graphs, this checker looks up the asset's exact policy version and
 * reports:
 * <ul>
 *   <li>{@code derivatives} when the policy has {@code derivatives=false}, for any relation;</li>
 *   <li>{@code aiTraining} when the policy has {@code aiTraining=false}, only for inputs whose
 *       relation is {@code trained-on}.</li>
 * </ul>
 *
 * <p>Conflicts are only reported, never used to reject: Module C stores the edge anyway and
 * keeps the conflicts on it. A node reached through several inputs (for example a shared base in
 * a diamond) is reported once per field. The result is sorted by asset id, version and field so
 * responses are stable.
 */
@Component
@Transactional(readOnly = true)
public class PolicyDerivationConflictChecker implements DerivationConflictChecker {

    static final String DERIVATIVES = "derivatives";
    static final String AI_TRAINING = "aiTraining";

    private static final Comparator<PolicyConflict> ORDER = Comparator
            .comparing((PolicyConflict conflict) -> conflict.source().assetId())
            .thenComparingInt(conflict -> conflict.source().version())
            .thenComparing(PolicyConflict::field);

    private final AssetLookup assets;
    private final PolicyLookup policies;

    PolicyDerivationConflictChecker(AssetLookup assets, PolicyLookup policies) {
        this.assets = assets;
        this.policies = policies;
    }

    @Override
    public List<PolicyConflict> check(
            String clientId,
            List<DerivationInput> inputs,
            Map<AssetRef, LineageGraph> upstreamByInput) {
        // A set removes repeats when two inputs share an ancestor.
        Set<PolicyConflict> conflicts = new LinkedHashSet<>();
        for (DerivationInput input : inputs) {
            boolean trainedOn = input.relation() == RelationType.TRAINED_ON;
            for (AssetRef node : upstreamOf(input, upstreamByInput)) {
                findPolicy(clientId, node).ifPresent(policy -> {
                    if (!policy.derivatives()) {
                        conflicts.add(new PolicyConflict(node, policy.ref(), DERIVATIVES));
                    }
                    if (trainedOn && !policy.aiTraining()) {
                        conflicts.add(new PolicyConflict(node, policy.ref(), AI_TRAINING));
                    }
                });
            }
        }
        return conflicts.stream().sorted(ORDER).toList();
    }

    /** The input's upstream nodes; if C sent no graph for it, the input alone is checked. */
    private static Set<AssetRef> upstreamOf(
            DerivationInput input, Map<AssetRef, LineageGraph> upstreamByInput) {
        LineageGraph graph = upstreamByInput == null ? null : upstreamByInput.get(input.asset());
        if (graph == null || graph.nodes() == null) {
            return Set.of(input.asset());
        }
        Set<AssetRef> nodes = new LinkedHashSet<>(graph.nodes());
        nodes.add(input.asset());
        return nodes;
    }

    /**
     * The exact policy version linked to the asset. Empty if either is missing, which cannot
     * happen for assets registered through the API (registration checks the policy exists);
     * such a node is skipped because conflicts must never make a derivation fail.
     */
    private Optional<PolicyView> findPolicy(String clientId, AssetRef node) {
        return assets.findAsset(clientId, node)
                .flatMap(asset -> policies.findPolicy(clientId, asset.policy()));
    }
}
