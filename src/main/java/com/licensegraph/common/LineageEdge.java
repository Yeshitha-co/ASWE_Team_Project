package com.licensegraph.common;

/**
 * Directed derivation edge stored input to output (Section 10).
 *
 * <p>Lives in common so Module B can read graphs without depending on Module C.
 */
public record LineageEdge(AssetRef input, AssetRef output, RelationType relation) {
}
