package com.licensegraph.asset;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link Asset} (Module B, task B1).
 *
 * <p>Package-private so other modules cannot reach it; they use {@code AssetLookup} instead.
 * Every query takes the owning {@code clientId}, so one client never sees another's assets.
 *
 * <p>AI-assisted (Claude): drafted with Claude, reviewed by the Module B owner.
 */
interface AssetRepository extends JpaRepository<Asset, Long> {

    Optional<Asset> findByClientIdAndAssetIdAndVersion(String clientId, String assetId, int version);

    boolean existsByClientIdAndAssetIdAndVersion(String clientId, String assetId, int version);

    List<Asset> findByClientIdAndPolicyIdAndPolicyVersion(String clientId, String policyId, int policyVersion);
}
