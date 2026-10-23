package com.licensegraph.security;

import com.licensegraph.common.error.ApiError;
import com.licensegraph.common.exception.ApiException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Builds the Section 10 error body used by the exception handler and the API-key filter.
 */
final class ApiErrorFactory {

    private ApiErrorFactory() {
    }

    static ApiError from(ApiException exception, String path) {
        return of(exception.status(), exception.code().name(), exception.getMessage(), path, exception.details());
    }

    static ApiError of(int status, String code, String message, String path, Map<String, Object> details) {
        Map<String, Object> bodyDetails = details == null || details.isEmpty() ? null : details;
        return new ApiError(
                Instant.now().truncatedTo(ChronoUnit.SECONDS),
                status,
                code,
                message,
                path,
                bodyDetails);
    }
}
