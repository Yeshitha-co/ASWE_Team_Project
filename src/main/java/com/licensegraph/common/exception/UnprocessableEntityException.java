package com.licensegraph.common.exception;

/**
 * HTTP 422 failures: the body references something not in the caller's scope.
 */
public class UnprocessableEntityException extends ApiException {

    public UnprocessableEntityException(ErrorCode code, String message) {
        super(require422(code), message);
    }

    public static UnprocessableEntityException unknownPolicy(String policyName) {
        return new UnprocessableEntityException(
                ErrorCode.UNKNOWN_POLICY_REFERENCE, "Unknown policy reference: " + policyName);
    }

    public static UnprocessableEntityException unknownInputAsset(String assetName) {
        return new UnprocessableEntityException(
                ErrorCode.UNKNOWN_INPUT_ASSET, "Unknown input asset: " + assetName);
    }

    private static ErrorCode require422(ErrorCode code) {
        if (code.status() != 422) {
            throw new IllegalArgumentException(code + " is not a 422 code");
        }
        return code;
    }
}
