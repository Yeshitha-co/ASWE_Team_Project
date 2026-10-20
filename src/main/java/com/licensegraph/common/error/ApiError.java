package com.licensegraph.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

/**
 * Shared error body used by every endpoint (Section 10).
 *
 * <p>Example: {@code CYCLE_DETECTED} with {@code details.cyclePath}. Module E maps exceptions
 * onto this type in task E5; no controller should build error JSON by hand.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        Map<String, Object> details) {
}
