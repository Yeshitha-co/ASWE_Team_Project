package com.licensegraph.decision;

import com.licensegraph.common.Obligation;
import com.licensegraph.common.PolicyRef;
import java.time.Instant;
import java.util.List;

/**
 * Public JSON response for a recorded intended-use decision.
 *
 * <p>Contains the decision identifier, evaluation timestamp,
 * and the complete evaluation result.
 *
 * @param decisionId unique identifier of the recorded decision
 * @param evaluatedAt timestamp when the decision was evaluated
 * @param result evaluated permissions, blocking paths, and obligations
 */
public record UseDecisionResponse(
        String decisionId,
        Instant evaluatedAt,
        ResultResponse result) {

    /**
     * Converts an internal decision record into an API response.
     *
     * @param record stored decision record
     * @return response containing the recorded evaluation result
     */
    public static UseDecisionResponse from(UseDecisionRecord record) {
        return new UseDecisionResponse(
                record.decisionId(),
                record.evaluatedAt(),
                ResultResponse.from(record.result()));
    }

    /**
     * Represents the evaluation result returned by the API.
     *
     * <p>Asset and policy references are represented as strings.
     * Decision values are returned in uppercase.
     *
     * @param target evaluated asset reference
     * @param use intended use requested by the client
     * @param decision ALLOW or DENY
     * @param blockingPaths paths explaining denied permissions
     * @param evaluatedPolicies policies considered during evaluation
     * @param obligations license obligations associated with the result
     */
    public record ResultResponse(
            String target,
            String use,
            String decision,
            List<BlockingPathResponse> blockingPaths,
            List<String> evaluatedPolicies,
            List<ObligationResponse> obligations) {

        static ResultResponse from(UseDecisionResult result) {
            return new ResultResponse(
                    result.target().toString(),
                    result.use().toJson(),
                    result.decision().name(),
                    result.blockingPaths().stream()
                            .map(BlockingPathResponse::from)
                            .toList(),
                    result.evaluatedPolicies().stream()
                            .map(PolicyRef::toString)
                            .toList(),
                    result.obligations().stream()
                            .map(ObligationResponse::from)
                            .toList());
        }
    }

    /**
     * Represents a lineage path explaining a denied intended use.
     *
     * @param path ordered asset references in the blocking lineage
     * @param blockingPolicy policy responsible for the denial
     * @param blockingField policy field that caused the denial
     */
    public record BlockingPathResponse(
            List<String> path,
            String blockingPolicy,
            String blockingField) {

        static BlockingPathResponse from(BlockingPath blockingPath) {
            return new BlockingPathResponse(
                    blockingPath.path().stream()
                            .map(Object::toString)
                            .toList(),
                    blockingPath.blockingPolicy().toString(),
                    blockingPath.blockingField());
        }
    }

    /**
     * Represents a license obligation included in a decision.
     *
     * @param type type of obligation
     * @param source asset that introduces the obligation
     * @param policy policy associated with the obligation
     */
    public record ObligationResponse(
            String type,
            String source,
            String policy) {

        static ObligationResponse from(Obligation obligation) {
            return new ObligationResponse(
                    obligation.type().toJson(),
                    obligation.source().toString(),
                    obligation.policy().toString());
        }
    }
}