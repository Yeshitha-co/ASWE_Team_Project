
package com.licensegraph.decision;

import com.licensegraph.common.Decision;
import com.licensegraph.policy.RuleOutcome;

import java.util.List;

/**
 * Combines individual policy evaluation results into a final use decision.
 *
 * <p>Applies a deny-overrides rule: if any evaluated policy denies
 * the requested use, the overall decision is DENY. Otherwise,
 * the overall decision is ALLOW.
 *
 * <p>This class only combines existing policy outcomes. It does not
 * retrieve policies, evaluate individual license permissions, or
 * persist decisions.
 *
 */
public class PolicyEvaluation {

    /**
     * Determines the final decision from a collection of policy outcomes.
     *
     * <p>Returns DENY immediately when a blocking policy is found.
     * If all outcomes permit the requested use, returns ALLOW.
     *
     * @param outcomes results of individual policy checks
     * @return DENY if any outcome is disallowed; otherwise ALLOW
     */
    public Decision evaluate(List<RuleOutcome> outcomes) {

        // A single denying policy is sufficient to reject the use.
        for (RuleOutcome outcome : outcomes) {
            if (!outcome.allowed()) {
                return Decision.DENY;
            }
        }

        // No blocking policy was found.
        return Decision.ALLOW;
    }
}
