package com.licensegraph.asset;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.PolicyRef;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI-assisted: {@link AssetLookup} backed by the asset table (Module B, task B5).
 *
 * <p>Used by Module C to check derivation inputs and by Module D to load the asset being
 * evaluated. Every lookup is scoped to the given client, so another client's asset is simply
 * absent. Callers only ever see {@link AssetView}, never the JPA entity.
 */
@Component
@Transactional(readOnly = true)
public class JpaAssetLookup implements AssetLookup {

    private final AssetRepository repository;

    JpaAssetLookup(AssetRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AssetView> findAsset(String clientId, AssetRef ref) {
        return repository.findByClientIdAndAssetIdAndVersion(clientId, ref.assetId(), ref.version())
                .map(Asset::toView);
    }

    @Override
    public List<AssetView> findAssetsByPolicy(String clientId, PolicyRef policy) {
        // Sorted by asset id, then version, so impact analysis results are deterministic.
        List<Asset> linked = repository.findByClientIdAndPolicyIdAndPolicyVersion(
                clientId, policy.policyId(), policy.version());
        return linked.stream()
                .map(Asset::toView)
                .sorted(Comparator.comparing((AssetView view) -> view.ref().assetId())
                        .thenComparingInt(view -> view.ref().version()))
                .toList();
    }
}
