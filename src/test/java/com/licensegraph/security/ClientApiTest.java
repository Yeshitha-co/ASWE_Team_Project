package com.licensegraph.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.licensegraph.support.ApiTestBase;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * REST Assured checks for client registration, key storage, and caller identity (E8).
 *
 * <p>Policy, asset, edge, and decision 404 isolation cannot run here. Those endpoints are still on
 * the Module A, B, C, and D branches.
 */
@Import({ModuleEWebTest.CallerProbe.class, ModuleEWebTest.ProbeService.class})
class ClientApiTest extends ApiTestBase {

    @Autowired
    private ApiClientRepository clients;

    @Test
    void registrationReturnsKeyOnceAndStoresOnlyItsHash() {
        ApiClient stored = clients.findById(clientId).orElseThrow();

        assertThat(apiKey).isNotBlank();
        assertThat(stored.getKeyHash()).isEqualTo(ApiKeyHasher.sha256Hex(apiKey));
        assertThat(stored.getKeyHash()).isNotEqualTo(apiKey);
        assertThat(clients.findAll()).noneMatch(client -> apiKey.equals(client.getKeyHash()));
    }

    @Test
    void registrationIgnoresClientIdInTheBody() {
        String createdId = RestAssured.given()
                .contentType("application/json")
                .body("{\"clientId\":\"attacker\"}")
                .when()
                .post("/api/clients")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getString("clientId");

        assertThat(createdId).isNotEqualTo("attacker");
    }

    @Test
    void validKeyReachesTheServiceAsTheRegisteredClient() {
        asCaller()
                .when()
                .get("/api/test/caller")
                .then()
                .statusCode(200)
                .body("clientId", equalTo(clientId));
    }

    @Test
    void missingKeyReturns401() {
        RestAssured.given()
                .when()
                .get("/api/test/caller")
                .then()
                .statusCode(401)
                .body("status", equalTo(401))
                .body("code", equalTo("MISSING_API_KEY"))
                .body("path", equalTo("/api/test/caller"))
                .body("message", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    void invalidKeyReturns401() {
        RestAssured.given()
                .header(ApiKeyFilter.HEADER, "not-a-real-key")
                .when()
                .get("/api/test/caller")
                .then()
                .statusCode(401)
                .body("status", equalTo(401))
                .body("code", equalTo("INVALID_API_KEY"))
                .body("path", equalTo("/api/test/caller"));
    }

    @Test
    void secondClientGetsADifferentIdentity() {
        Response other = registerAnotherClient();
        String otherId = other.jsonPath().getString("clientId");
        String otherKey = other.jsonPath().getString("apiKey");

        assertThat(otherId).isNotEqualTo(clientId);
        assertThat(otherKey).isNotEqualTo(apiKey);

        RestAssured.given()
                .header(ApiKeyFilter.HEADER, otherKey)
                .when()
                .get("/api/test/caller")
                .then()
                .statusCode(200)
                .body("clientId", equalTo(otherId));
    }
}
