package com.licensegraph.decision;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.licensegraph.common.Decision;
import com.licensegraph.policy.RuleOutcome;
import java.util.List;
import org.junit.jupiter.api.Test;

class PolicyEvaluationTest {

    @Test
    void shouldDenyIfAnyPolicyBlocks() {
        PolicyEvaluation evaluation = new PolicyEvaluation();

        List<RuleOutcome> outcomes = List.of(
                new RuleOutcome(false, "commercialUse"),
                new RuleOutcome(true, null),
                new RuleOutcome(true, null)
        );

        Decision result = evaluation.evaluate(outcomes);

        assertEquals(Decision.DENY, result);
    }

    @Test
    void shouldAllowIfAllPoliciesAllow() {
        PolicyEvaluation evaluation = new PolicyEvaluation();

        List<RuleOutcome> outcomes = List.of(
                new RuleOutcome(true, null),
                new RuleOutcome(true, null)
        );

        Decision result = evaluation.evaluate(outcomes);

        assertEquals(Decision.ALLOW, result);
    }
}