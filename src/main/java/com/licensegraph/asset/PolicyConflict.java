package com.licensegraph.asset;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.PolicyRef;

/**
 * An upstream policy field that conflicts with a new derivation (Section 10, Module B).
 *
 * <p>Conflicts are reported and stored; they never reject the derivation.
 */
public record PolicyConflict(AssetRef source, PolicyRef policy, String field) {
}
