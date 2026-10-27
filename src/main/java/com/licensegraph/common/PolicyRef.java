package com.licensegraph.common;

/**
 * Versioned policy reference. Prints as {@code license-A:v1} (Section 10).
 *
 * <p>JSON uses the standard object shape {@code {"policyId":"license-A","version":1}} so request
 * bodies can nest this record without a custom serializer.
 */
public record PolicyRef(String policyId, int version) {

    /** Requires a non-blank policy id and a version of at least 1, then trims the id. */
    public PolicyRef {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("policyId is required");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be a positive integer");
        }
        policyId = policyId.trim();
    }

    @Override
    public String toString() {
        return policyId + ":v" + version;
    }
}
