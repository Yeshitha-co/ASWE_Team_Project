package com.licensegraph.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.AssetType;
import com.licensegraph.common.PolicyRef;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Repository tests for {@link Asset} against in-memory H2 (Module B, task B1).
 *
 * <p>AI-assisted (Claude): tests drafted with Claude, reviewed by the Module B owner.
 */
@DataJpaTest
class AssetRepositoryTest {

    private static final AssetRef DATASET_A_V1 = new AssetRef("dataset-A", 1);
    private static final PolicyRef LICENSE_A_V1 = new PolicyRef("license-A", 1);

    @Autowired
    private AssetRepository repository;

    @Test
    void savesAndReadsBackAssetWithExactPolicyVersion() {
        repository.saveAndFlush(new Asset("client-1", DATASET_A_V1, AssetType.DATASET, LICENSE_A_V1, "source: hub"));

        Asset found = repository.findByClientIdAndAssetIdAndVersion("client-1", "dataset-A", 1).orElseThrow();

        assertThat(found.getRef()).isEqualTo(DATASET_A_V1);
        assertThat(found.getType()).isEqualTo(AssetType.DATASET);
        assertThat(found.getPolicy()).isEqualTo(LICENSE_A_V1);
        assertThat(found.getMetadata()).isEqualTo("source: hub");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.toView()).isEqualTo(new AssetView(DATASET_A_V1, AssetType.DATASET, LICENSE_A_V1));
    }

    @Test
    void sameAssetVersionTwiceForOneClientViolatesUniqueConstraint() {
        repository.saveAndFlush(new Asset("client-1", DATASET_A_V1, AssetType.DATASET, LICENSE_A_V1, null));

        assertThatThrownBy(() -> repository.saveAndFlush(
                new Asset("client-1", DATASET_A_V1, AssetType.MODEL, LICENSE_A_V1, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void differentClientsMayOwnTheSameAssetVersion() {
        repository.saveAndFlush(new Asset("client-1", DATASET_A_V1, AssetType.DATASET, LICENSE_A_V1, null));
        repository.saveAndFlush(new Asset("client-2", DATASET_A_V1, AssetType.DATASET, LICENSE_A_V1, null));

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void lookupsAreScopedToTheOwningClient() {
        repository.saveAndFlush(new Asset("client-1", DATASET_A_V1, AssetType.DATASET, LICENSE_A_V1, null));

        assertThat(repository.findByClientIdAndAssetIdAndVersion("client-2", "dataset-A", 1)).isEmpty();
        assertThat(repository.existsByClientIdAndAssetIdAndVersion("client-2", "dataset-A", 1)).isFalse();
        assertThat(repository.findByClientIdAndPolicyIdAndPolicyVersion("client-2", "license-A", 1)).isEmpty();
    }

    @Test
    void findsAssetsByExactPolicyVersion() {
        repository.saveAndFlush(new Asset("client-1", DATASET_A_V1, AssetType.DATASET, LICENSE_A_V1, null));
        repository.saveAndFlush(new Asset("client-1", new AssetRef("dataset-A", 2), AssetType.DATASET,
                new PolicyRef("license-A", 2), null));

        assertThat(repository.findByClientIdAndPolicyIdAndPolicyVersion("client-1", "license-A", 1))
                .extracting(Asset::getRef)
                .containsExactly(DATASET_A_V1);
    }
}
