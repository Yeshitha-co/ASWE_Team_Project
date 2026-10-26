package com.licensegraph.support;

import static io.restassured.RestAssured.given;

import com.licensegraph.security.ApiKeyFilter;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * AI-assisted: REST Assured test infrastructure (E6).
 *
 * <p>Starts the application, registers a client through {@code POST /api/clients}, and exposes
 * {@link #asCaller()} with that client's {@code X-API-Key}. Other modules should extend this class
 * instead of starting their own server or inventing a key.
 *
 * <p>This base only supplies the callers. Cross-client checks are in
 * {@link com.licensegraph.security.ClientIsolationApiTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ApiTestBase {

    @LocalServerPort
    private int port;

    protected String clientId;

    protected String apiKey;

    @BeforeEach
    void registerClient() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        Response created = given()
                .when()
                .post("/api/clients")
                .then()
                .statusCode(201)
                .extract()
                .response();
        clientId = created.jsonPath().getString("clientId");
        apiKey = created.jsonPath().getString("apiKey");
    }

    /**
     * A request that sends the registered client's API key.
     */
    protected RequestSpecification asCaller() {
        return given().header(ApiKeyFilter.HEADER, apiKey);
    }

    /**
     * Registers a second client and returns its raw API key. The key is not stored.
     */
    protected Response registerAnotherClient() {
        return given()
                .when()
                .post("/api/clients")
                .then()
                .statusCode(201)
                .extract()
                .response();
    }
}
