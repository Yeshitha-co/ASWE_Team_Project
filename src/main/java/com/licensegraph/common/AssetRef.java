package com.licensegraph.common;

/**
 * Versioned asset reference. Prints as {@code model-C:v1} (Section 10).
 *
 * <p>JSON uses the standard object shape {@code {"assetId":"model-C","version":1}} so request
 * bodies can nest this record without a custom serializer.
 */
public record AssetRef(String assetId, int version) {

    /** Requires a non-blank asset id and a version of at least 1, then trims the id. */
    public AssetRef {
        if (assetId == null || assetId.isBlank()) {
            throw new IllegalArgumentException("assetId is required");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be a positive integer");
        }
        assetId = assetId.trim();
    }

    @Override
    public String toString() {
        return assetId + ":v" + version;
    }
}
