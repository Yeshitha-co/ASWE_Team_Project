package com.licensegraph.common.exception;

import com.licensegraph.common.AssetRef;
import java.util.List;
import java.util.Map;

/**
 * HTTP 409 failures: duplicate versions or edges, and cycle detection.
 *
 * <p>{@code DerivationService} throws this type for {@code CYCLE_DETECTED} and
 * {@code DUPLICATE_EDGE}; the whole derivation batch must roll back.
 */
public class ConflictException extends ApiException {

    public ConflictException(ErrorCode code, String message) {
        super(require409(code), message);
    }

    public ConflictException(ErrorCode code, String message, Map<String, Object> details) {
        super(require409(code), message, details);
    }

    public static ConflictException cycleDetected(List<AssetRef> cyclePath) {
        List<String> namedPath = cyclePath.stream().map(AssetRef::toString).toList();
        String message = "Edge would create a cycle";
        if (namedPath.size() >= 2) {
            message = "Edge " + namedPath.get(namedPath.size() - 2) + " -> " + namedPath.get(namedPath.size() - 1)
                    + " would create a cycle";
        }
        return new ConflictException(
                ErrorCode.CYCLE_DETECTED, message, Map.of("cyclePath", namedPath));
    }

    public static ConflictException duplicateEdge() {
        return new ConflictException(ErrorCode.DUPLICATE_EDGE, "Derivation edge already exists");
    }

    public static ConflictException duplicatePolicyVersion(String policyName) {
        return new ConflictException(
                ErrorCode.DUPLICATE_POLICY_VERSION, "Policy version already exists: " + policyName);
    }

    public static ConflictException duplicateAssetVersion(String assetName) {
        return new ConflictException(
                ErrorCode.DUPLICATE_ASSET_VERSION, "Asset version already exists: " + assetName);
    }

    private static ErrorCode require409(ErrorCode code) {
        if (code.status() != 409) {
            throw new IllegalArgumentException(code + " is not a 409 code");
        }
        return code;
    }
}
