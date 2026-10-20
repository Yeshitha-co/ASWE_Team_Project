package com.licensegraph.asset;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.PolicyRef;
import java.util.List;
import java.util.Optional;

/**
 * Client-scoped asset lookup so other modules never touch JPA classes (Section 10).
 *
 * <p>Implemented by Module B. Used by C (input checks) and D. {@code findAssetsByPolicy} is for
 * impact analysis in Iteration 2.
 */
public interface AssetLookup {

    Optional<AssetView> findAsset(String clientId, AssetRef ref);

    List<AssetView> findAssetsByPolicy(String clientId, PolicyRef policy);
}
