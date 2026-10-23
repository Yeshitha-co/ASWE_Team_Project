package com.licensegraph.lineage;

import com.licensegraph.asset.AssetLookup;
import com.licensegraph.common.AssetRef;
import com.licensegraph.common.LineageEdge;
import com.licensegraph.common.LineageGraph;
import com.licensegraph.common.exception.ErrorCode;
import com.licensegraph.common.exception.NotFoundException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Answers ancestry and descendant queries by breadth-first search over stored edges (C6).
 *
 * <p>Each asset is visited once, so a shared ancestor in a diamond appears once in the result.
 * Every lookup is scoped to the caller's client.
 */

@Service
public class LineageQueryServiceImpl implements LineageQueryService {

    private final DerivationEdgeRepository edges;
    private final AssetLookup assets;

    public LineageQueryServiceImpl(DerivationEdgeRepository edges, AssetLookup assets) {
        this.edges = edges;
        this.assets = assets;
    }

    @Override
    @Transactional(readOnly = true)
    public LineageGraph findAncestors(String clientId, AssetRef target) {
        requireAsset(clientId, target);
        return walk(clientId, target, true);
    }

    @Override
    @Transactional(readOnly = true)
    public LineageGraph findDescendants(String clientId, AssetRef source) {
        requireAsset(clientId, source);
        return walk(clientId, source, false);
    }

    /**
     * Throws 404 ASSET_NOT_FOUND when the asset is unknown or belongs to another client.
     */
    private void requireAsset(String clientId, AssetRef ref) {
        if (assets.findAsset(clientId, ref).isEmpty()) {
            throw new NotFoundException(ErrorCode.ASSET_NOT_FOUND, "Asset not found: " + ref);
        }
    }

    /**
     * Breadth-first walk from the root. Upward follows output to input (ancestors);
     * downward follows input to output (descendants). The root is included in the nodes.
     */
    private LineageGraph walk(String clientId, AssetRef root, boolean upward) {
        Set<AssetRef> visited = new LinkedHashSet<>();
        List<LineageEdge> found = new ArrayList<>();
        Deque<AssetRef> queue = new ArrayDeque<>();

        visited.add(root);
        queue.add(root);

        while (!queue.isEmpty()) {
            AssetRef current = queue.poll();
            for (DerivationEdge edge : neighbours(clientId, current, upward)) {
                found.add(edge.toLineageEdge());
                AssetRef next = upward ? edge.input() : edge.output();
                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }
        return new LineageGraph(root, visited, found);
    }

    /**
     * Direct parents when walking up, direct children when walking down.
     */
    private List<DerivationEdge> neighbours(String clientId, AssetRef node, boolean upward) {
        if (upward) {
            return edges.findByClientIdAndOutputAssetIdAndOutputVersion(
                    clientId, node.assetId(), node.version());
        }
        return edges.findByClientIdAndInputAssetIdAndInputVersion(
                clientId, node.assetId(), node.version());
    }
}