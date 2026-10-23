
package com.licensegraph.decision;

import com.licensegraph.common.Obligation;
import com.licensegraph.common.PolicyRef;
import java.time.Instant;
import java.util.List;

/**
 * Public JSON response for a recorded intended-use decision.
 */
public record UseDecisionResponse(
        String decisionId,
        Instant evaluatedAt,
        ResultResponse result) {

    public static UseDecisionResponse from(UseDecisionRecord record) {
        return new UseDecisionResponse(
                record.decisionId(),
                record.evaluatedAt(),
                ResultResponse.from(record.result()));
    }

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
