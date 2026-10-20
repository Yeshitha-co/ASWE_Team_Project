package com.licensegraph.common;

/**
 * A duty inherited from a policy on a source asset (Section 10).
 */
public record Obligation(ObligationType type, AssetRef source, PolicyRef policy) {
}
