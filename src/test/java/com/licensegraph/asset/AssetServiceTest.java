package com.licensegraph.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.AssetType;
import com.licensegraph.common.PolicyRef;
import com.licensegraph.common.exception.ApiException;
import com.licensegraph.common.exception.ErrorCode;
import com.licensegraph.policy.PolicyLookup;
import com.licensegraph.policy.PolicyView;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * AI-assisted: Unit tests for {@link AssetService} with Mockito doubles for the repository and
 * Module A's {@link PolicyLookup} (Module B, tasks B2 to B4).
 */
@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    private static final String CLIENT = "client-1";
    private static final PolicyRef LICENSE_A_V1 = new PolicyRef("license-A", 1);
    private static final PolicyView LICENSE_A_V1_VIEW =
            new PolicyView(LICENSE_A_V1, false, true, true, true, true, false);

    @Mock
    private AssetRepository repository;

    @Mock
    private PolicyLookup policyLookup;

    private AssetService service;

    @BeforeEach
    void setUp() {
        service = new AssetService(repository, policyLookup);
    }

    private static RegisterAssetRequest datasetA(String metadata) {
        return new RegisterAssetRequest("dataset-A", 1, "dataset", "license-A", 1, metadata);
    }

    @Test
    void registersValidAssetLinkedToExactPolicyVersion() {
        when(repository.existsByClientIdAndAssetIdAndVersion(CLIENT, "dataset-A", 1))
                .thenReturn(false);
        when(policyLookup.findPolicy(CLIENT, LICENSE_A_V1))
                .thenReturn(Optional.of(LICENSE_A_V1_VIEW));
        when(repository.saveAndFlush(any(Asset.class))).thenAnswer(call -> call.getArgument(0));

        AssetResponse response = service.register(CLIENT, datasetA("source: hub"));

        assertThat(response.name()).isEqualTo("dataset-A:v1");
        assertThat(response.type()).isEqualTo(AssetType.DATASET);
        assertThat(response.policy()).isEqualTo("license-A:v1");
        assertThat(response.metadata()).isEqualTo("source: hub");
        assertThat(response.createdAt()).isNotNull();

        ArgumentCaptor<Asset> saved = ArgumentCaptor.forClass(Asset.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getClientId()).isEqualTo(CLIENT);
        assertThat(saved.getValue().getRef()).isEqualTo(new AssetRef("dataset-A", 1));
        assertThat(saved.getValue().getPolicy()).isEqualTo(LICENSE_A_V1);
    }

    @Test
    void acceptsKebabCaseDerivedDatasetType() {
        when(policyLookup.findPolicy(CLIENT, LICENSE_A_V1))
                .thenReturn(Optional.of(LICENSE_A_V1_VIEW));
        when(repository.saveAndFlush(any(Asset.class))).thenAnswer(call -> call.getArgument(0));

        AssetResponse response = service.register(CLIENT,
                new RegisterAssetRequest("subset-E", 1, "derived-dataset", "license-A", 1, null));

        assertThat(response.type()).isEqualTo(AssetType.DERIVED_DATASET);
    }

    @Test
    void unknownPolicyVersionGives422AndStoresNothing() {
        when(policyLookup.findPolicy(CLIENT, new PolicyRef("license-A", 9)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(CLIENT,
                new RegisterAssetRequest("dataset-A", 1, "dataset", "license-A", 9, null)))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.code()).isEqualTo(ErrorCode.UNKNOWN_POLICY_REFERENCE);
                    assertThat(ex.status()).isEqualTo(422);
                    assertThat(ex.getMessage()).contains("license-A:v9");
                });
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void repeatedAssetVersionGives409() {
        when(repository.existsByClientIdAndAssetIdAndVersion(CLIENT, "dataset-A", 1))
                .thenReturn(true);

        assertThatThrownBy(() -> service.register(CLIENT, datasetA(null)))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.code()).isEqualTo(ErrorCode.DUPLICATE_ASSET_VERSION);
                    assertThat(ex.status()).isEqualTo(409);
                    assertThat(ex.getMessage()).contains("dataset-A:v1");
                });
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(policyLookup);
    }

    @Test
    void simultaneousDuplicateCaughtByUniqueConstraintGives409() {
        when(policyLookup.findPolicy(CLIENT, LICENSE_A_V1))
                .thenReturn(Optional.of(LICENSE_A_V1_VIEW));
        when(repository.saveAndFlush(any(Asset.class)))
                .thenThrow(new DataIntegrityViolationException("uk_assets_client_asset_version"));

        assertThatThrownBy(() -> service.register(CLIENT, datasetA(null)))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.code()).isEqualTo(ErrorCode.DUPLICATE_ASSET_VERSION));
    }

    @Test
    void unknownTypeGives400InvalidAssetType() {
        assertThatThrownBy(() -> service.register(CLIENT,
                new RegisterAssetRequest("dataset-A", 1, "spreadsheet", "license-A", 1, null)))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.code()).isEqualTo(ErrorCode.INVALID_ASSET_TYPE);
                    assertThat(ex.status()).isEqualTo(400);
                });
        verifyNoInteractions(repository, policyLookup);
    }

    private static RegisterAssetRequest request(String assetId, Integer version, String type,
            String policyId, Integer policyVersion, String metadata) {
        return new RegisterAssetRequest(assetId, version, type, policyId, policyVersion, metadata);
    }

    static Stream<Arguments> invalidRequests() {
        String tooLong = "x".repeat(AssetService.MAX_METADATA_LENGTH + 1);
        return Stream.of(
                Arguments.of("missing assetId",
                        request(null, 1, "dataset", "license-A", 1, null)),
                Arguments.of("blank assetId",
                        request("  ", 1, "dataset", "license-A", 1, null)),
                Arguments.of("slash in assetId",
                        request("my/model", 1, "model", "license-A", 1, null)),
                Arguments.of("colon in assetId",
                        request("model:v2", 1, "model", "license-A", 1, null)),
                Arguments.of("space in assetId",
                        request("my model", 1, "model", "license-A", 1, null)),
                Arguments.of("missing version",
                        request("dataset-A", null, "dataset", "license-A", 1, null)),
                Arguments.of("zero version",
                        request("dataset-A", 0, "dataset", "license-A", 1, null)),
                Arguments.of("negative version",
                        request("dataset-A", -1, "dataset", "license-A", 1, null)),
                Arguments.of("missing type",
                        request("dataset-A", 1, null, "license-A", 1, null)),
                Arguments.of("missing policyId",
                        request("dataset-A", 1, "dataset", null, 1, null)),
                Arguments.of("missing policyVersion",
                        request("dataset-A", 1, "dataset", "license-A", null, null)),
                Arguments.of("zero policyVersion",
                        request("dataset-A", 1, "dataset", "license-A", 0, null)),
                Arguments.of("metadata too long",
                        request("dataset-A", 1, "dataset", "license-A", 1, tooLong)),
                Arguments.of("no body", null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequests")
    void invalidRequestGives400ValidationError(String description, RegisterAssetRequest request) {
        assertThatThrownBy(() -> service.register(CLIENT, request))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.code()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    assertThat(ex.details()).containsKey("field");
                });
        verifyNoInteractions(repository, policyLookup);
    }

    @Test
    void metadataAtMaximumLengthIsAccepted() {
        when(policyLookup.findPolicy(CLIENT, LICENSE_A_V1))
                .thenReturn(Optional.of(LICENSE_A_V1_VIEW));
        when(repository.saveAndFlush(any(Asset.class))).thenAnswer(call -> call.getArgument(0));

        String metadata = "x".repeat(AssetService.MAX_METADATA_LENGTH);

        assertThat(service.register(CLIENT, datasetA(metadata)).metadata())
                .hasSize(AssetService.MAX_METADATA_LENGTH);
    }

    @Test
    void getReturnsStoredAsset() {
        AssetRef datasetA = new AssetRef("dataset-A", 1);
        Asset stored = new Asset(CLIENT, datasetA, AssetType.DATASET, LICENSE_A_V1, null);
        when(repository.findByClientIdAndAssetIdAndVersion(CLIENT, "dataset-A", 1))
                .thenReturn(Optional.of(stored));

        AssetResponse response = service.get(CLIENT, "dataset-A", 1);

        assertThat(response.name()).isEqualTo("dataset-A:v1");
        assertThat(response.policy()).isEqualTo("license-A:v1");
    }

    @Test
    void getMissingAssetGives404() {
        when(repository.findByClientIdAndAssetIdAndVersion(CLIENT, "dataset-A", 2))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(CLIENT, "dataset-A", 2))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.code()).isEqualTo(ErrorCode.ASSET_NOT_FOUND);
                    assertThat(ex.status()).isEqualTo(404);
                });
    }

    @Test
    void getNonPositiveVersionGives404WithoutQuerying() {
        assertThatThrownBy(() -> service.get(CLIENT, "dataset-A", 0))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.code()).isEqualTo(ErrorCode.ASSET_NOT_FOUND));
        verifyNoInteractions(repository);
    }
}
