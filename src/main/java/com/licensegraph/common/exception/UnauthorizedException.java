package com.licensegraph.common.exception;

/**
 * HTTP 401 failures: missing or invalid API key.
 */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(ErrorCode code, String message) {
        super(require401(code), message);
    }

    public static UnauthorizedException missingApiKey() {
        return new UnauthorizedException(ErrorCode.MISSING_API_KEY, "API key is required");
    }

    public static UnauthorizedException invalidApiKey() {
        return new UnauthorizedException(ErrorCode.INVALID_API_KEY, "API key is invalid");
    }

    private static ErrorCode require401(ErrorCode code) {
        if (code.status() != 401) {
            throw new IllegalArgumentException(code + " is not a 401 code");
        }
        return code;
    }
}
