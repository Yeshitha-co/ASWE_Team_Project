package com.licensegraph.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.licensegraph.common.error.ApiError;
import com.licensegraph.common.exception.ConflictException;
import com.licensegraph.common.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Guards the frozen Section 10 naming, JSON, and error-schema contracts.
 */
class SharedContractsTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void assetRefPrintsAsNameColonV() {
        assertThat(new AssetRef("model-C", 1).toString()).isEqualTo("model-C:v1");
    }

    @Test
    void policyRefPrintsAsNameColonV() {
        assertThat(new PolicyRef("license-A", 1).toString()).isEqualTo("license-A:v1");
    }

    @Test
    void versionMustBePositive() {
        assertThatThrownBy(() -> new AssetRef("model-C", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PolicyRef("license-A", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void enumsUseKebabCaseJson() throws Exception {
        assertThat(mapper.writeValueAsString(IntendedUse.AI_TRAINING)).isEqualTo("\"ai-training\"");
        assertThat(mapper.writeValueAsString(RelationType.FINE_TUNED_FROM)).isEqualTo("\"fine-tuned-from\"");
        assertThat(mapper.writeValueAsString(AssetType.DERIVED_DATASET)).isEqualTo("\"derived-dataset\"");
        assertThat(mapper.writeValueAsString(Decision.DENY)).isEqualTo("\"deny\"");
        assertThat(mapper.writeValueAsString(ObligationType.SHARE_ALIKE)).isEqualTo("\"share-alike\"");

        assertThat(mapper.readValue("\"ai-training\"", IntendedUse.class)).isEqualTo(IntendedUse.AI_TRAINING);
        assertThat(mapper.readValue("\"fine-tuned-from\"", RelationType.class))
                .isEqualTo(RelationType.FINE_TUNED_FROM);
    }

    @Test
    void refsUseObjectJsonShape() throws Exception {
        AssetRef asset = new AssetRef("model-C", 1);
        PolicyRef policy = new PolicyRef("license-A", 1);

        assertThat(mapper.writeValueAsString(asset)).isEqualTo("{\"assetId\":\"model-C\",\"version\":1}");
        assertThat(mapper.writeValueAsString(policy)).isEqualTo("{\"policyId\":\"license-A\",\"version\":1}");
        assertThat(mapper.readValue("{\"assetId\":\"model-C\",\"version\":1}", AssetRef.class)).isEqualTo(asset);
        assertThat(mapper.readValue("{\"policyId\":\"license-A\",\"version\":1}", PolicyRef.class)).isEqualTo(policy);
    }

    @Test
    void errorCodesMatchSection10Statuses() {
        assertThat(ErrorCode.VALIDATION_ERROR.status()).isEqualTo(400);
        assertThat(ErrorCode.INVALID_INTENDED_USE.status()).isEqualTo(400);
        assertThat(ErrorCode.INVALID_POLICY_COMBINATION.status()).isEqualTo(400);
        assertThat(ErrorCode.INVALID_ASSET_TYPE.status()).isEqualTo(400);
        assertThat(ErrorCode.MISSING_API_KEY.status()).isEqualTo(401);
        assertThat(ErrorCode.INVALID_API_KEY.status()).isEqualTo(401);
        assertThat(ErrorCode.ASSET_NOT_FOUND.status()).isEqualTo(404);
        assertThat(ErrorCode.POLICY_NOT_FOUND.status()).isEqualTo(404);
        assertThat(ErrorCode.DECISION_NOT_FOUND.status()).isEqualTo(404);
        assertThat(ErrorCode.DUPLICATE_POLICY_VERSION.status()).isEqualTo(409);
        assertThat(ErrorCode.DUPLICATE_ASSET_VERSION.status()).isEqualTo(409);
        assertThat(ErrorCode.DUPLICATE_EDGE.status()).isEqualTo(409);
        assertThat(ErrorCode.CYCLE_DETECTED.status()).isEqualTo(409);
        assertThat(ErrorCode.UNKNOWN_POLICY_REFERENCE.status()).isEqualTo(422);
        assertThat(ErrorCode.UNKNOWN_INPUT_ASSET.status()).isEqualTo(422);
    }

    @Test
    void cycleExceptionCarriesCyclePathDetails() {
        ConflictException ex = ConflictException.cycleDetected(List.of(
                new AssetRef("dataset-A", 1),
                new AssetRef("model-C", 1),
                new AssetRef("model-B", 1),
                new AssetRef("dataset-A", 1)));
        assertThat(ex.code()).isEqualTo(ErrorCode.CYCLE_DETECTED);
        assertThat(ex.status()).isEqualTo(409);
        assertThat(ex.details()).containsEntry(
                "cyclePath", List.of("dataset-A:v1", "model-C:v1", "model-B:v1", "dataset-A:v1"));
    }

    @Test
    void apiErrorJsonMatchesSection10Shape() throws Exception {
        ApiError error = new ApiError(
                Instant.parse("2026-10-21T14:03:11Z"),
                409,
                "CYCLE_DETECTED",
                "Edge dataset-A:v1 -> model-C:v1 would create a cycle",
                "/api/derivations",
                Map.of("cyclePath", List.of("dataset-A:v1", "model-C:v1", "model-B:v1", "dataset-A:v1")));

        String json = mapper.writeValueAsString(error);
        assertThat(json).contains("\"timestamp\":\"2026-10-21T14:03:11Z\"");
        assertThat(json).contains("\"status\":409");
        assertThat(json).contains("\"code\":\"CYCLE_DETECTED\"");
        assertThat(json).contains("\"path\":\"/api/derivations\"");
        assertThat(json).contains("\"cyclePath\"");
    }
}
