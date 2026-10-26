package com.licensegraph.asset;

import static org.assertj.core.api.Assertions.assertThat;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.AssetType;
import com.licensegraph.common.PolicyRef;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * AI-assisted: Tests for {@link JpaAssetLookup} against in-memory H2 (Module B, task B5).
 */
@DataJpaTest
@Import(JpaAssetLookup.class)
class JpaAssetLookupTest {

    private static final AssetRef DATASET_A_V1 = new AssetRef("dataset-A", 1);
    private static final PolicyRef LICENSE_A_V1 = new PolicyRef("license-A", 1);

    @Autowired
    private AssetRepository repository;

    @Autowired
    private AssetLookup lookup;

    @Test
    void findsOwnAssetAsView() {
        repository.saveAndFlush(
                new Asset("client-1", DATASET_A_V1, AssetType.DATASET, LICENSE_A_V1, null));

        assertThat(lookup.findAsset("client-1", DATASET_A_V1))
                .contains(new AssetView(DATASET_A_V1, AssetType.DATASET, LICENSE_A_V1));
    }

    @Test
    void missingAssetOrVersionIsEmpty() {
        repository.saveAndFlush(
                new Asset("client-1", DATASET_A_V1, AssetType.DATASET, LICENSE_A_V1, null));

        assertThat(lookup.findAsset("client-1", new AssetRef("dataset-A", 2))).isEmpty();
        assertThat(lookup.findAsset("client-1", new AssetRef("model-B", 1))).isEmpty();
    }

    @Test
    void anotherClientsAssetIsEmpty() {
        repository.saveAndFlush(
                new Asset("client-1", DATASET_A_V1, AssetType.DATASET, LICENSE_A_V1, null));

        assertThat(lookup.findAsset("client-2", DATASET_A_V1)).isEmpty();
    }

    @Test
    void findsAssetsByExactPolicyVersionForCallerOnlyInOrder() {
        AssetRef modelB = new AssetRef("model-B", 1);
        AssetRef datasetAv2 = new AssetRef("dataset-A", 2);
        repository.saveAndFlush(new Asset("client-1", modelB, AssetType.MODEL, LICENSE_A_V1, null));
        repository.saveAndFlush(
                new Asset("client-1", datasetAv2, AssetType.DATASET, LICENSE_A_V1, null));
        repository.saveAndFlush(
                new Asset("client-1", DATASET_A_V1, AssetType.DATASET, LICENSE_A_V1, null));
        repository.saveAndFlush(new Asset("client-1", new AssetRef("code-D", 1), AssetType.CODE,
                new PolicyRef("license-A", 2), null));
        repository.saveAndFlush(
                new Asset("client-2", DATASET_A_V1, AssetType.DATASET, LICENSE_A_V1, null));

        assertThat(lookup.findAssetsByPolicy("client-1", LICENSE_A_V1))
                .extracting(AssetView::ref)
                .containsExactly(DATASET_A_V1, datasetAv2, modelB);
    }
}
