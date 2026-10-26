package com.licensegraph.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import com.licensegraph.support.ApiTestBase;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * AI-assisted: E4 cross-client integration tests.
 *
 * <p>Covers the integrated policy, asset, edge, and decision routes.
 *
 * <p>Client A is the caller registered by {@link ApiTestBase}. Client B has a separate key.
 * Status codes follow Section 10: a foreign read is 404, a body that names another client's
 * policy or asset is 422, and a missing or unknown key is 401.
 */
class ClientIsolationApiTest extends ApiTestBase {

    private String otherKey;

    @BeforeEach
    void registerSecondClient() {
        otherKey = registerAnotherClient().jsonPath().getString("apiKey");
    }

    @Test
    void secondClientCannotReadOrOverwriteFirstClientPolicy() {
        asCaller()
                .contentType("application/json")
                .body(policyBody("license-A", false))
                .when()
                .post("/api/policies")
                .then()
                .statusCode(201);

        asOther().when().get("/api/policies/license-A/versions/1").then()
                .statusCode(404)
                .body("code", equalTo("POLICY_NOT_FOUND"));
        asOther().when().get("/api/policies/license-A/versions").then()
                .statusCode(404)
                .body("code", equalTo("POLICY_NOT_FOUND"));

        asOther()
                .contentType("application/json")
                .body(policyBody("license-A", true))
                .when()
                .post("/api/policies")
                .then()
                .statusCode(201);

        asCaller().when().get("/api/policies/license-A/versions/1").then()
                .statusCode(200)
                .body("commercialUse", equalTo(false));
    }

    @Test
    void secondClientCannotReadOrReferenceFirstClientAsset() {
        createPolicy("license-A", false);
        asCaller()
                .contentType("application/json")
                .body(assetBody("dataset-A", "dataset", "license-A"))
                .when()
                .post("/api/assets")
                .then()
                .statusCode(201);

        asOther().when().get("/api/assets/dataset-A/versions/1").then()
                .statusCode(404)
                .body("code", equalTo("ASSET_NOT_FOUND"));

        asOther()
                .contentType("application/json")
                .body(assetBody("dataset-B", "dataset", "license-A"))
                .when()
                .post("/api/assets")
                .then()
                .statusCode(422)
                .body("code", equalTo("UNKNOWN_POLICY_REFERENCE"));
    }

    @Test
    void secondClientCannotReadOrReferenceFirstClientEdges() {
        createChain();

        asOther().when().get("/api/assets/dataset-A/versions/1/ancestors").then()
                .statusCode(404)
                .body("code", equalTo("ASSET_NOT_FOUND"));
        asOther().when().get("/api/assets/model-C/versions/1/descendants").then()
                .statusCode(404)
                .body("code", equalTo("ASSET_NOT_FOUND"));

        createPolicyForOther("license-B");
        asOther()
                .contentType("application/json")
                .body(assetBody("model-B", "model", "license-B"))
                .when()
                .post("/api/assets")
                .then()
                .statusCode(201);
        asOther()
                .contentType("application/json")
                .body(derivationBody("model-B", "dataset-A", "trained-on"))
                .when()
                .post("/api/derivations")
                .then()
                .statusCode(422)
                .body("code", equalTo("UNKNOWN_INPUT_ASSET"));
    }

    @Test
    void secondClientCannotReadOrReferenceFirstClientDecision() {
        createChain();
        String decisionId = asCaller()
                .contentType("application/json")
                .body("{\"assetId\":\"model-C\",\"version\":1,\"intendedUse\":\"commercial\"}")
                .when()
                .post("/api/use-decisions")
                .then()
                .statusCode(201)
                .extract()
                .path("decisionId");

        asOther().when().get("/api/use-decisions/" + decisionId).then()
                .statusCode(404)
                .body("code", equalTo("DECISION_NOT_FOUND"));
        asOther()
                .contentType("application/json")
                .body("{\"assetId\":\"model-C\",\"version\":1,\"intendedUse\":\"commercial\"}")
                .when()
                .post("/api/use-decisions")
                .then()
                .statusCode(404)
                .body("code", equalTo("ASSET_NOT_FOUND"));
    }

    @Test
    void secondClientCannotListFirstClientDecisionHistory() {
        createChain();
        asCaller()
                .contentType("application/json")
                .body("{\"assetId\":\"model-C\",\"version\":1,\"intendedUse\":\"commercial\"}")
                .when()
                .post("/api/use-decisions")
                .then()
                .statusCode(201);

        // D7: another client's decision history is 404 DECISION_NOT_FOUND, never an empty 200.
        asOther()
                .queryParam("assetId", "model-C")
                .queryParam("version", 1)
                .when()
                .get("/api/use-decisions")
                .then()
                .statusCode(404)
                .body("code", equalTo("DECISION_NOT_FOUND"));
    }

    @Test
    void missingAndInvalidKeysAre401() {
        String[] paths = {
                "/api/policies/license-A/versions/1",
                "/api/assets/dataset-A/versions/1",
                "/api/assets/dataset-A/versions/1/ancestors",
                "/api/use-decisions/missing"
        };
        for (String path : paths) {
            given().when().get(path).then()
                    .statusCode(401)
                    .body("code", equalTo("MISSING_API_KEY"))
                    .body("code", not(equalTo("INVALID_API_KEY")));
            given().header(ApiKeyFilter.HEADER, "not-a-real-key").when().get(path).then()
                    .statusCode(401)
                    .body("code", equalTo("INVALID_API_KEY"));
        }
    }

    private RequestSpecification asOther() {
        return given().header(ApiKeyFilter.HEADER, otherKey);
    }

    private void createChain() {
        createPolicy("license-A", false);
        createPolicy("license-B", true);
        createPolicy("license-C", true);
        asCaller().contentType("application/json")
                .body(assetBody("dataset-A", "dataset", "license-A"))
                .when().post("/api/assets").then().statusCode(201);
        asCaller().contentType("application/json").body(assetBody("model-B", "model", "license-B"))
                .when().post("/api/assets").then().statusCode(201);
        asCaller().contentType("application/json").body(assetBody("model-C", "model", "license-C"))
                .when().post("/api/assets").then().statusCode(201);
        asCaller().contentType("application/json")
                .body(derivationBody("model-B", "dataset-A", "trained-on"))
                .when().post("/api/derivations").then().statusCode(201);
        asCaller().contentType("application/json")
                .body(derivationBody("model-C", "model-B", "fine-tuned-from"))
                .when().post("/api/derivations").then().statusCode(201);
    }

    private void createPolicy(String policyId, boolean commercialUse) {
        asCaller().contentType("application/json").body(policyBody(policyId, commercialUse))
                .when().post("/api/policies").then().statusCode(201);
    }

    private void createPolicyForOther(String policyId) {
        asOther().contentType("application/json").body(policyBody(policyId, true))
                .when().post("/api/policies").then().statusCode(201);
    }

    private static String policyBody(String policyId, boolean commercialUse) {
        return "{"
                + "\"policyId\":\"" + policyId + "\","
                + "\"version\":1,"
                + "\"commercialUse\":" + commercialUse + ","
                + "\"redistribution\":true,"
                + "\"derivatives\":true,"
                + "\"aiTraining\":true,"
                + "\"attributionRequired\":true,"
                + "\"shareAlike\":false"
                + "}";
    }

    private static String assetBody(String assetId, String type, String policyId) {
        return "{"
                + "\"assetId\":\"" + assetId + "\","
                + "\"version\":1,"
                + "\"type\":\"" + type + "\","
                + "\"policyId\":\"" + policyId + "\","
                + "\"policyVersion\":1"
                + "}";
    }

    private static String derivationBody(String outputId, String inputId, String relation) {
        return "{"
                + "\"output\":{\"assetId\":\"" + outputId + "\",\"version\":1},"
                + "\"inputs\":[{\"assetId\":\"" + inputId + "\",\"version\":1,"
                + "\"relation\":\"" + relation + "\"}]"
                + "}";
    }
}
