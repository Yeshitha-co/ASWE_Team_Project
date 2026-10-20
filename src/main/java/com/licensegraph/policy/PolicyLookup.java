package com.licensegraph.policy;

import com.licensegraph.common.PolicyRef;
import java.util.Optional;

/**
 * Client-scoped policy lookup so other modules never touch JPA classes (Section 10).
 *
 * <p>Implemented by Module A. Used by B (asset validation) and D (decisions).
 */
public interface PolicyLookup {

    Optional<PolicyView> findPolicy(String clientId, PolicyRef ref);
}
