package com.licensegraph.decision;

import java.time.Instant;

/**
 * Stored decision history row (Section 10, Module D). Insert-only.
 *
 * <p>AI-assisted: AI assistance was used to review and document
 * the decision record structure and improve code clarity.
 *
 */
public record UseDecisionRecord(String decisionId, Instant evaluatedAt, UseDecisionResult result) {
}
