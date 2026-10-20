package com.licensegraph.decision;

import java.time.Instant;

/**
 * Stored decision history row (Section 10, Module D). Insert-only.
 */
public record UseDecisionRecord(String decisionId, Instant evaluatedAt, UseDecisionResult result) {
}
