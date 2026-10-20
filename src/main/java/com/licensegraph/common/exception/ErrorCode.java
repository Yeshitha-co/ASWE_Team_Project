package com.licensegraph.common.exception;

/**
 * Frozen error codes and HTTP statuses from Section 10.
 *
 * <p>Cross-client access uses the matching 404 code, never 403.
 */
public enum ErrorCode {
    VALIDATION_ERROR(400),
    INVALID_INTENDED_USE(400),
    INVALID_POLICY_COMBINATION(400),
    INVALID_ASSET_TYPE(400),

    MISSING_API_KEY(401),
    INVALID_API_KEY(401),

    ASSET_NOT_FOUND(404),
    POLICY_NOT_FOUND(404),
    DECISION_NOT_FOUND(404),

    DUPLICATE_POLICY_VERSION(409),
    DUPLICATE_ASSET_VERSION(409),
    DUPLICATE_EDGE(409),
    CYCLE_DETECTED(409),

    UNKNOWN_POLICY_REFERENCE(422),
    UNKNOWN_INPUT_ASSET(422);

    private final int status;

    ErrorCode(int status) {
        this.status = status;
    }

    public int status() {
        return status;
    }
}
