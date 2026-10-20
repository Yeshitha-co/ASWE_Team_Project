package com.licensegraph.common.exception;

import java.util.Collections;
import java.util.Map;

/**
 * Base typed API failure. Carries the Section 10 code, status, and optional details map.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, Object> details;

    public ApiException(ErrorCode code, String message) {
        this(code, message, Map.of());
    }

    public ApiException(ErrorCode code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details == null ? Map.of() : Collections.unmodifiableMap(details);
    }

    public ErrorCode code() {
        return code;
    }

    public int status() {
        return code.status();
    }

    public Map<String, Object> details() {
        return details;
    }
}
