
package com.licensegraph.decision;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.LineageEdge;
import com.licensegraph.common.LineageGraph;
import com.licensegraph.common.PolicyRef;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Constructs an explanation path for a policy that blocks an intended use.
 *
 * <p>Uses breadth-first search (BFS) to find a shortest lineage path
 * from the requested target asset to the asset whose policy denies
 * the use.
 *
 * <p>Lineage edges are stored from input to output. This builder
 * traverses them in reverse because a blocking policy may belong
 * to an ancestor of the requested asset.
 *
 */
public class BlockingPathBuilder {

    /**
     * Finds a shortest path from the target to a blocking asset.
     *
     * <p>BFS explores paths in increasing order of their length,
     * so the first path reaching the blocking asset is a shortest
     * path in terms of the number of lineage edges.
     *
     * @param graph lineage graph containing the target and its ancestors
     * @param target asset for which the intended use was requested
     * @param blockingAsset asset whose policy denies the requested use
     * @param blockingPolicy policy responsible for the denial
     * @param blockingField policy field responsible for the denial
     * @return blocking path together with its policy and blocking field
     * @throws IllegalArgumentException if no path to the blocking asset exists
     */
    public BlockingPath build(
            LineageGraph graph,
            AssetRef target,
            AssetRef blockingAsset,
            PolicyRef blockingPolicy,
            String blockingField) {

        // Each queue entry represents a candidate path starting at the target.
        // Initialize BFS with a path containing only the target asset.
        Queue<List<AssetRef>> queue = new ArrayDeque<>();
        queue.add(List.of(target));

        // Avoid exploring the same asset more than once.
        // This also prevents infinite loops if the graph contains a cycle.
        Set<AssetRef> visited = new HashSet<>();
        visited.add(target);

        // BFS processes shorter paths before longer paths.
        while (!queue.isEmpty()) {

            // Remove the oldest candidate path from the queue.
            List<AssetRef> path = queue.poll();

            // The final asset in the path is the node being explored.
            AssetRef current = path.get(path.size() - 1);

            // Stop when the asset responsible for the denial is reached.
            if (current.equals(blockingAsset)) {
                return new BlockingPath(
                        path,
                        blockingPolicy,
                        blockingField
                );
            }

            // Find parents by traversing lineage edges backward:
            // an edge's input is a parent of its output.
            for (LineageEdge edge : graph.edges()) {

                if (edge.output().equals(current)) {
                    AssetRef parent = edge.input();

                    // Add a parent only on its first discovery.
                    if (visited.add(parent)) {

                        // Copy the existing path before appending the parent.
                        // This preserves the paths already in the queue.
                        List<AssetRef> newPath = new ArrayList<>(path);
                        newPath.add(parent);

                        // Explore this extended path in a later BFS iteration.
                        queue.add(newPath);
                    }
                }
            }
        }

        // No lineage route connects the target to the blocking asset.
        throw new IllegalArgumentException(
                "No lineage path found to blocking asset: " + blockingAsset
        );
    }
}
