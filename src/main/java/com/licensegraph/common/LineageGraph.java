package com.licensegraph.common;

import java.util.List;
import java.util.Set;

/**
 * Subgraph of nodes and edges rooted at one asset version (Section 10).
 *
 * <p>Lives in common so Module B can read graphs without depending on Module C.
 */
public record LineageGraph(AssetRef root, Set<AssetRef> nodes, List<LineageEdge> edges) {
}
