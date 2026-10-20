package com.licensegraph.policy;

import com.licensegraph.common.PolicyRef;

/**
 * Read model for one stored policy version (Section 10, Module A).
 */
public record PolicyView(
        PolicyRef ref,
        boolean commercialUse,
        boolean redistribution,
        boolean derivatives,
        boolean aiTraining,
        boolean attributionRequired,
        boolean shareAlike) {
}
