package com.licensegraph.decision;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for creating a new use decision.
 *
 * <p>Used by POST /api/use-decisions to identify the target asset
 * version and intended use that must be evaluated.
 *
 * <p>Bean Validation ensures that the asset identifier is not blank,
 * the version is positive, and the intended use is provided.
 *
 * <p>The authenticated client identifier is intentionally excluded
 * from this request. It is obtained through ClientContext instead.
 *
 * @param assetId non-blank identifier of the target asset
 * @param version positive version number of the target asset
 * @param intendedUse intended use to evaluate
 */
public record CreateUseDecisionRequest(
        @NotBlank String assetId,
        @Min(1) int version,
        @NotBlank String intendedUse
) {
}