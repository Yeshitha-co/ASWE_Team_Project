package com.licensegraph.asset;

import com.licensegraph.common.AssetType;
import java.time.Instant;

/**
 * Response body for asset registration and lookup (Module B, tasks B2 and B4).
 *
 * <p>Example: {@code {"assetId":"dataset-A","version":1,"name":"dataset-A:v1","type":"dataset",
 * "policyId":"license-A","policyVersion":1,"policy":"license-A:v1","createdAt":"..."}}.
 * {@code metadata} is left out when it was not supplied.
 *
 * <p>AI-assisted (Claude): drafted with Claude, reviewed by the Module B owner.
 *
 * @param assetId asset identifier
 * @param version asset version
 * @param name printed asset reference, e.g. {@code dataset-A:v1}
 * @param type asset type, serialized in kebab-case
 * @param policyId identifier of the linked policy
 * @param policyVersion exact linked policy version
 * @param policy printed policy reference, e.g. {@code license-A:v1}
 * @param metadata optional free-text metadata
 * @param createdAt when this asset version was registered
 */
public record AssetResponse(
        String assetId,
        int version,
        String name,
        AssetType type,
        String policyId,
        int policyVersion,
        String policy,
        String metadata,
        Instant createdAt) {

    /** Builds the response from a stored asset. */
    static AssetResponse from(Asset asset) {
        return new AssetResponse(
                asset.getRef().assetId(),
                asset.getRef().version(),
                asset.getRef().toString(),
                asset.getType(),
                asset.getPolicy().policyId(),
                asset.getPolicy().version(),
                asset.getPolicy().toString(),
                asset.getMetadata(),
                asset.getCreatedAt());
    }
}
