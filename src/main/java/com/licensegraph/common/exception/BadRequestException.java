package com.licensegraph.common.exception;

import java.util.Map;

/**
 * HTTP 400 failures: validation, intended use, policy combination, or asset type.
 */
public class BadRequestException extends ApiException {

    public BadRequestException(ErrorCode code, String message) {
        this(code, message, Map.of());
    }

    public BadRequestException(ErrorCode code, String message, Map<String, Object> details) {
        super(requireStatus(code, 400), message, details);
    }

    private static ErrorCode requireStatus(ErrorCode code, int status) {
        if (code.status() != status) {
            throw new IllegalArgumentException(code + " is not a " + status + " code");
        }
        return code;
    }
}
