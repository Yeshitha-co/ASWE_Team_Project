package com.licensegraph.asset;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.AssetType;
import com.licensegraph.common.PolicyRef;

/**
 * Read model for one stored asset version (Section 10, Module B).
 */
public record AssetView(AssetRef ref, AssetType type, PolicyRef policy) {
}
