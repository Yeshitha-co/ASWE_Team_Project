package com.licensegraph.asset;

/**
 * Request body for {@code POST /api/assets} (Module B, task B2).
 *
 * <p>Fields are boxed and {@code type} is a plain string so that missing or unknown values reach
 * {@link AssetService}, which reports them with the exact error codes from Section 10
 * ({@code VALIDATION_ERROR}, {@code INVALID_ASSET_TYPE}) instead of a generic parse failure.
 * There is no {@code clientId} field: the owner always comes from the caller's API key.
 *
 * <p>AI-assisted (Claude): drafted with Claude, reviewed by the Module B owner.
 *
 * @param assetId asset identifier, e.g. {@code dataset-A}
 * @param version asset version, a positive integer
 * @param type one of {@code dataset}, {@code model}, {@code code}, {@code derived-dataset}
 * @param policyId identifier of the policy this asset version carries
 * @param policyVersion exact version of that policy
 * @param metadata optional free-text name or source reference
 */
public record RegisterAssetRequest(
        String assetId,
        Integer version,
        String type,
        String policyId,
        Integer policyVersion,
        String metadata) {
}
