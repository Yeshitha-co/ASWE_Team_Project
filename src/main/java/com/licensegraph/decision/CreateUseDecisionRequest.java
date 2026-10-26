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
 * <p>AI-assisted: AI assistance was used to refine request validation,
 * handle unsupported intended-use values, and improve documentation
 * and Checkstyle compliance.
 *
 * @param assetId non-blank identifier of the target asset
 * @param version positive version number of the target asset
 * @param intendedUse intended use to evaluate, as its JSON value (for example
 *     {@code commercial}); kept as text so an unknown value is reported as
 *     {@code INVALID_INTENDED_USE} rather than a generic parse error
 *
 */
public record CreateUseDecisionRequest(
        @NotBlank String assetId,
        @Min(1) int version,
        @NotBlank String intendedUse
) {
}