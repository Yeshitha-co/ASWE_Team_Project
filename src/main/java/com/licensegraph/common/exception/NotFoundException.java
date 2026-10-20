package com.licensegraph.common.exception;

/**
 * HTTP 404 failures, including another client's records (never 403).
 *
 * <p>{@code LineageQueryService} throws this type when the target is missing.
 */
public class NotFoundException extends ApiException {

    public NotFoundException(ErrorCode code, String message) {
        super(require404(code), message);
    }

    public static NotFoundException asset(String clientScopedName) {
        return new NotFoundException(ErrorCode.ASSET_NOT_FOUND, "Asset not found: " + clientScopedName);
    }

    public static NotFoundException policy(String clientScopedName) {
        return new NotFoundException(ErrorCode.POLICY_NOT_FOUND, "Policy not found: " + clientScopedName);
    }

    public static NotFoundException decision(String decisionId) {
        return new NotFoundException(ErrorCode.DECISION_NOT_FOUND, "Decision not found: " + decisionId);
    }

    private static ErrorCode require404(ErrorCode code) {
        if (code.status() != 404) {
            throw new IllegalArgumentException(code + " is not a 404 code");
        }
        return code;
    }
}
