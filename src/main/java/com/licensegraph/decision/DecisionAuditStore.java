package com.licensegraph.decision;

import com.licensegraph.common.AssetRef;
import java.util.List;
import java.util.Optional;

/**
 * Insert-only decision history (Section 10, Module D).
 *
 * <p>The JPA repository stays private so no update or delete leaks out.
 */
public interface DecisionAuditStore {

    UseDecisionRecord save(String clientId, UseDecisionResult result);

    Optional<UseDecisionRecord> findById(String clientId, String decisionId);

    List<UseDecisionRecord> findByTarget(String clientId, AssetRef target);
}
