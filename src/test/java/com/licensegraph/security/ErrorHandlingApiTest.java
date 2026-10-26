package com.licensegraph.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.licensegraph.support.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

/**
 * REST Assured checks that shared failures and unexpected failures use the error
 * schema (E5, E8).
 */
@Import(SecurityWebTest.ErrorProbe.class)
class ErrorHandlingApiTest extends ApiTestBase {

    @Test
    void sharedNotFoundUsesSection10Body() {
        asCaller()
                .when()
                .get("/api/test/errors/typed/not-found")
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("code", equalTo("ASSET_NOT_FOUND"))
                .body("path", equalTo("/api/test/errors/typed/not-found"))
                .body("timestamp", notNullValue());
    }

    @Test
    void unexpectedExceptionDoesNotLeakInternals() {
        String body = asCaller()
                .when()
                .get("/api/test/errors/unexpected")
                .then()
                .statusCode(500)
                .body("status", equalTo(500))
                .body("code", equalTo(GlobalExceptionHandler.INTERNAL_ERROR))
                .body("message", equalTo("An unexpected error occurred"))
                .body("path", equalTo("/api/test/errors/unexpected"))
                .extract()
                .asString();

        assertThat(body)
                .doesNotContain("db-password-should-not-leak")
                .doesNotContain("IllegalStateException");
    }
}
