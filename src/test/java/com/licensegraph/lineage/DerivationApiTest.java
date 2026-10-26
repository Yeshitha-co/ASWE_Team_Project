package com.licensegraph.lineage;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.licensegraph.support.ApiTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * AI-assisted: REST Assured API tests for the Module C endpoints over HTTP.
 *
 * <p>Uses Module E's {@link ApiTestBase}, which starts the application on a random port and
 * registers a fresh client before each test. Policies and assets are registered through Modules
 * A and B's real endpoints. Covers one success case per endpoint and every error code that
 * POST /api/derivations and the ancestors and descendants endpoints return.
 */
class DerivationApiTest extends ApiTestBase {

    private static final String MODEL_B_FROM_DATASET_A = """
            { "output": { "assetId": "model-B", "version": 1 },
              "inputs": [ { "assetId": "dataset-A", "version": 1, "relation": "trained-on" } ] }
            """;

    private static final String MODEL_C_FROM_MODEL_B = """
            { "output": { "assetId": "model-C", "version": 1 },
              "inputs": [ { "assetId": "model-B", "version": 1,
                            "relation": "fine-tuned-from" } ] }
            """;

    /** Registers license-A and the three assets of the A-B-C chain for the caller. */
    @BeforeEach
    void registerPolicyAndAssets() {
        asCaller()
                .contentType("application/json")
                .body("""
                        { "policyId": "license-A", "version": 1, "commercialUse": false,
                          "redistribution": true, "derivatives": true, "aiTraining": true,
                          "attributionRequired": true, "shareAlike": false }
                        """)
                .when()
                .post("/api/policies")
                .then()
                .statusCode(201);
        registerAsset("dataset-A", "dataset");
        registerAsset("model-B", "model");
        registerAsset("model-C", "model");
    }

    private void registerAsset(String assetId, String type) {
        asCaller()
                .contentType("application/json")
                .body("""
                        { "assetId": "%s", "version": 1, "type": "%s",
                          "policyId": "license-A", "policyVersion": 1 }
                        """.formatted(assetId, type))
                .when()
                .post("/api/assets")
                .then()
                .statusCode(201);
    }

    private void recordDerivation(String body) {
        asCaller()
                .contentType("application/json")
                .body(body)
                .when()
                .post("/api/derivations")
                .then()
                .statusCode(201);
    }

    @Test
    void recordsDerivationAndReturnsCreatedEdge() {
        asCaller()
                .contentType("application/json")
                .body(MODEL_B_FROM_DATASET_A)
                .when()
                .post("/api/derivations")
                .then()
                .statusCode(201)
                .body("output", equalTo("model-B:v1"))
                .body("created", hasSize(1))
                .body("created[0].input", equalTo("dataset-A:v1"))
                .body("created[0].output", equalTo("model-B:v1"))
                .body("created[0].relation", equalTo("trained-on"))
                .body("ancestorCount", equalTo(1));
    }

    @Test
    void ancestorsOfLastModelReachBackToDataset() {
        recordDerivation(MODEL_B_FROM_DATASET_A);
        recordDerivation(MODEL_C_FROM_MODEL_B);

        asCaller()
                .when()
                .get("/api/assets/model-C/versions/1/ancestors")
                .then()
                .statusCode(200)
                .body("root", equalTo("model-C:v1"))
                .body("nodes", contains("dataset-A:v1", "model-B:v1", "model-C:v1"))
                .body("edges", hasSize(2));
    }

    @Test
    void descendantsOfDatasetReachLastModel() {
        recordDerivation(MODEL_B_FROM_DATASET_A);
        recordDerivation(MODEL_C_FROM_MODEL_B);

        asCaller()
                .when()
                .get("/api/assets/dataset-A/versions/1/descendants")
                .then()
                .statusCode(200)
                .body("root", equalTo("dataset-A:v1"))
                .body("nodes", contains("dataset-A:v1", "model-B:v1", "model-C:v1"))
                .body("edges", hasSize(2));
    }

    @Test
    void emptyInputsGive400() {
        asCaller()
                .contentType("application/json")
                .body("""
                        { "output": { "assetId": "model-B", "version": 1 }, "inputs": [] }
                        """)
                .when()
                .post("/api/derivations")
                .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void unknownRelationGives400() {
        asCaller()
                .contentType("application/json")
                .body("""
                        { "output": { "assetId": "model-B", "version": 1 },
                          "inputs": [ { "assetId": "dataset-A", "version": 1,
                                        "relation": "copied-from" } ] }
                        """)
                .when()
                .post("/api/derivations")
                .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void missingApiKeyGives401() {
        given()
                .when()
                .get("/api/assets/model-C/versions/1/ancestors")
                .then()
                .statusCode(401)
                .body("code", equalTo("MISSING_API_KEY"));
    }

    @Test
    void unknownAssetGives404() {
        asCaller()
                .when()
                .get("/api/assets/ghost/versions/1/descendants")
                .then()
                .statusCode(404)
                .body("code", equalTo("ASSET_NOT_FOUND"));
    }

    @Test
    void anotherClientsAssetGives404() {
        recordDerivation(MODEL_B_FROM_DATASET_A);
        String otherKey = registerAnotherClient().jsonPath().getString("apiKey");

        given()
                .header("X-API-Key", otherKey)
                .when()
                .get("/api/assets/model-B/versions/1/ancestors")
                .then()
                .statusCode(404)
                .body("code", equalTo("ASSET_NOT_FOUND"));
    }

    @Test
    void cycleGives409WithPathAndSavesNothing() {
        recordDerivation(MODEL_B_FROM_DATASET_A);
        recordDerivation(MODEL_C_FROM_MODEL_B);

        asCaller()
                .contentType("application/json")
                .body("""
                        { "output": { "assetId": "dataset-A", "version": 1 },
                          "inputs": [ { "assetId": "model-C", "version": 1,
                                        "relation": "derived-from" } ] }
                        """)
                .when()
                .post("/api/derivations")
                .then()
                .statusCode(409)
                .body("code", equalTo("CYCLE_DETECTED"))
                .body("details.cyclePath",
                        contains("dataset-A:v1", "model-C:v1", "model-B:v1", "dataset-A:v1"));

        // The rejected edge was not stored: dataset-A still has no ancestors.
        asCaller()
                .when()
                .get("/api/assets/dataset-A/versions/1/ancestors")
                .then()
                .statusCode(200)
                .body("nodes", contains("dataset-A:v1"))
                .body("edges", hasSize(0));
    }

    @Test
    void duplicateEdgeGives409() {
        recordDerivation(MODEL_B_FROM_DATASET_A);

        asCaller()
                .contentType("application/json")
                .body(MODEL_B_FROM_DATASET_A)
                .when()
                .post("/api/derivations")
                .then()
                .statusCode(409)
                .body("code", equalTo("DUPLICATE_EDGE"));
    }

    @Test
    void unregisteredInputGives422() {
        asCaller()
                .contentType("application/json")
                .body("""
                        { "output": { "assetId": "model-B", "version": 1 },
                          "inputs": [ { "assetId": "ghost", "version": 1,
                                        "relation": "trained-on" } ] }
                        """)
                .when()
                .post("/api/derivations")
                .then()
                .statusCode(422)
                .body("code", equalTo("UNKNOWN_INPUT_ASSET"));
    }
}