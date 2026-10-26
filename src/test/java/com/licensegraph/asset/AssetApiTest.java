package com.licensegraph.asset;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import com.licensegraph.support.ApiTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * AI-assisted: REST Assured API tests for the asset endpoints over HTTP (Module B, task B10).
 *
 * <p>Uses Module E's {@link ApiTestBase}, which starts the application on a random port and
 * registers a fresh client with its own API key before each test, so tests never share data.
 * Policies and derivations go through Modules A and C's real endpoints. Covers one success case
 * and every error code the asset endpoints return.
 */
class AssetApiTest extends ApiTestBase {

    private static final String DATASET_A = """
            { "assetId": "dataset-A", "version": 1, "type": "dataset",
              "policyId": "license-A", "policyVersion": 1 }
            """;

    @BeforeEach
    void registerLicenseA() {
        registerPolicy("license-A", 1, true, true);
    }

    /** Registers a policy for the caller; every field not named here allows its use. */
    private void registerPolicy(String policyId, int version, boolean derivatives,
            boolean aiTraining) {
        asCaller()
                .contentType("application/json")
                .body("""
                        { "policyId": "%s", "version": %d, "commercialUse": false,
                          "redistribution": true, "derivatives": %b, "aiTraining": %b,
                          "attributionRequired": true, "shareAlike": false }
                        """.formatted(policyId, version, derivatives, aiTraining))
                .when()
                .post("/api/policies")
                .then()
                .statusCode(201);
    }

    private void registerAsset(String body) {
        asCaller()
                .contentType("application/json")
                .body(body)
                .when()
                .post("/api/assets")
                .then()
                .statusCode(201);
    }

    @Test
    void registersAssetAndReadsItBack() {
        asCaller()
                .contentType("application/json")
                .body(DATASET_A)
                .when()
                .post("/api/assets")
                .then()
                .statusCode(201)
                .header("Location", "/api/assets/dataset-A/versions/1")
                .body("name", equalTo("dataset-A:v1"))
                .body("type", equalTo("dataset"))
                .body("policy", equalTo("license-A:v1"))
                .body("createdAt", notNullValue());

        asCaller()
                .when()
                .get("/api/assets/dataset-A/versions/1")
                .then()
                .statusCode(200)
                .body("assetId", equalTo("dataset-A"))
                .body("version", equalTo(1))
                .body("policyId", equalTo("license-A"))
                .body("policyVersion", equalTo(1));
    }

    @Test
    void oldVersionIsUnchangedAfterNewVersionWithAnotherPolicy() {
        registerPolicy("license-A", 2, true, true);
        registerAsset(DATASET_A);
        registerAsset("""
                { "assetId": "dataset-A", "version": 2, "type": "dataset",
                  "policyId": "license-A", "policyVersion": 2 }
                """);

        asCaller()
                .when()
                .get("/api/assets/dataset-A/versions/1")
                .then()
                .statusCode(200)
                .body("policy", equalTo("license-A:v1"));
    }

    @Test
    void repeatedVersionGives409() {
        registerAsset(DATASET_A);

        asCaller()
                .contentType("application/json")
                .body(DATASET_A)
                .when()
                .post("/api/assets")
                .then()
                .statusCode(409)
                .body("status", equalTo(409))
                .body("code", equalTo("DUPLICATE_ASSET_VERSION"))
                .body("message", equalTo("Asset version already exists: dataset-A:v1"))
                .body("path", equalTo("/api/assets"))
                .body("timestamp", notNullValue());
    }

    @Test
    void unknownPolicyVersionGives422() {
        asCaller()
                .contentType("application/json")
                .body("""
                        { "assetId": "dataset-A", "version": 1, "type": "dataset",
                          "policyId": "license-A", "policyVersion": 9 }
                        """)
                .when()
                .post("/api/assets")
                .then()
                .statusCode(422)
                .body("code", equalTo("UNKNOWN_POLICY_REFERENCE"))
                .body("message", equalTo("Unknown policy reference: license-A:v9"));
    }

    @Test
    void anotherClientsPolicyIsUnknownToo() {
        String otherKey = registerAnotherClient().jsonPath().getString("apiKey");

        given()
                .header("X-API-Key", otherKey)
                .contentType("application/json")
                .body(DATASET_A)
                .when()
                .post("/api/assets")
                .then()
                .statusCode(422)
                .body("code", equalTo("UNKNOWN_POLICY_REFERENCE"));
    }

    @Test
    void unknownTypeGives400InvalidAssetType() {
        asCaller()
                .contentType("application/json")
                .body("""
                        { "assetId": "dataset-A", "version": 1, "type": "spreadsheet",
                          "policyId": "license-A", "policyVersion": 1 }
                        """)
                .when()
                .post("/api/assets")
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_ASSET_TYPE"))
                .body("details.field", equalTo("type"));
    }

    @Test
    void missingFieldGives400ValidationError() {
        asCaller()
                .contentType("application/json")
                .body("""
                        { "version": 1, "type": "dataset", "policyId": "license-A",
                          "policyVersion": 1 }
                        """)
                .when()
                .post("/api/assets")
                .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"))
                .body("details.field", equalTo("assetId"));
    }

    @Test
    void idWithSlashGives400ValidationError() {
        asCaller()
                .contentType("application/json")
                .body("""
                        { "assetId": "my/model", "version": 1, "type": "model",
                          "policyId": "license-A", "policyVersion": 1 }
                        """)
                .when()
                .post("/api/assets")
                .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"))
                .body("details.field", equalTo("assetId"));
    }

    @Test
    void nonNumericVersionGives400ValidationError() {
        asCaller()
                .contentType("application/json")
                .body("""
                        { "assetId": "dataset-A", "version": "one", "type": "dataset",
                          "policyId": "license-A", "policyVersion": 1 }
                        """)
                .when()
                .post("/api/assets")
                .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void missingAssetGives404() {
        asCaller()
                .when()
                .get("/api/assets/dataset-A/versions/1")
                .then()
                .statusCode(404)
                .body("code", equalTo("ASSET_NOT_FOUND"))
                .body("path", equalTo("/api/assets/dataset-A/versions/1"));
    }

    @Test
    void anotherClientsAssetGives404Not403() {
        registerAsset(DATASET_A);
        String otherKey = registerAnotherClient().jsonPath().getString("apiKey");

        given()
                .header("X-API-Key", otherKey)
                .when()
                .get("/api/assets/dataset-A/versions/1")
                .then()
                .statusCode(404)
                .body("code", equalTo("ASSET_NOT_FOUND"));
    }

    @Test
    void requestWithoutKeyGives401() {
        given()
                .contentType("application/json")
                .body(DATASET_A)
                .when()
                .post("/api/assets")
                .then()
                .statusCode(401)
                .body("code", equalTo("MISSING_API_KEY"));
    }

    @Test
    void derivationReportsUpstreamConflictsDirectlyAndTwoGenerationsUp() {
        // dataset-A carries a policy that forbids derivatives and AI training.
        registerPolicy("no-derivatives", 1, false, false);
        registerPolicy("open", 1, true, true);
        registerAsset("""
                { "assetId": "dataset-A", "version": 1, "type": "dataset",
                  "policyId": "no-derivatives", "policyVersion": 1 }
                """);
        registerAsset("""
                { "assetId": "model-B", "version": 1, "type": "model",
                  "policyId": "open", "policyVersion": 1 }
                """);
        registerAsset("""
                { "assetId": "model-C", "version": 1, "type": "model",
                  "policyId": "open", "policyVersion": 1 }
                """);

        // Direct input, trained-on: both fields are reported, and the edge is still stored.
        asCaller()
                .contentType("application/json")
                .body("""
                        { "output": { "assetId": "model-B", "version": 1 },
                          "inputs": [ { "assetId": "dataset-A", "version": 1,
                                        "relation": "trained-on" } ] }
                        """)
                .when()
                .post("/api/derivations")
                .then()
                .statusCode(201)
                .body("created", hasSize(1))
                .body("conflicts", hasSize(2))
                .body("conflicts[0].source", equalTo("dataset-A:v1"))
                .body("conflicts[0].field", equalTo("aiTraining"))
                .body("conflicts[1].field", equalTo("derivatives"));

        // Two generations up, fine-tuned-from: only derivatives applies.
        asCaller()
                .contentType("application/json")
                .body("""
                        { "output": { "assetId": "model-C", "version": 1 },
                          "inputs": [ { "assetId": "model-B", "version": 1,
                                        "relation": "fine-tuned-from" } ] }
                        """)
                .when()
                .post("/api/derivations")
                .then()
                .statusCode(201)
                .body("conflicts", hasSize(1))
                .body("conflicts[0].source", equalTo("dataset-A:v1"))
                .body("conflicts[0].policy", equalTo("no-derivatives:v1"))
                .body("conflicts[0].field", equalTo("derivatives"));
    }
}
