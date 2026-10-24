package com.licensegraph.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.IntendedUse;
import com.licensegraph.common.Obligation;
import com.licensegraph.common.ObligationType;
import com.licensegraph.common.PolicyRef;
import java.util.List;
import org.junit.jupiter.api.Test;

class PolicyRulesImplTest {

    private final PolicyRulesImpl policyRules = new PolicyRulesImpl();

    private PolicyView createPolicy(
            boolean commercialUse,
            boolean redistribution,
            boolean derivatives,
            boolean aiTraining) {
        return new PolicyView(
                new PolicyRef("test-policy", 1),
                commercialUse,
                redistribution,
                derivatives,
                aiTraining,
                false,
                false);
    }

    @Test
    void checkAllowsUseWhenPolicyPermitsIt() {
        PolicyView policy = createPolicy(true, true, true, true);

        for (IntendedUse use : new IntendedUse[]{
                IntendedUse.COMMERCIAL,
                IntendedUse.REDISTRIBUTION,
                IntendedUse.DERIVATIVE,
                IntendedUse.AI_TRAINING}) {

            RuleOutcome result = policyRules.check(policy, use);

            assertTrue(result.allowed());
            assertEquals(null, result.blockingField());
        }
    }

    @Test
    void checkBlocksUseWhenPolicyForbidsIt() {
        PolicyView policy = createPolicy(false, false, false, false);

        assertBlocked(policy, IntendedUse.COMMERCIAL, "commercialUse");
        assertBlocked(policy, IntendedUse.REDISTRIBUTION, "redistribution");
        assertBlocked(policy, IntendedUse.DERIVATIVE, "derivatives");
        assertBlocked(policy, IntendedUse.AI_TRAINING, "aiTraining");
    }

    private void assertBlocked(
            PolicyView policy, IntendedUse use, String expectedField) {
        RuleOutcome result = policyRules.check(policy, use);

        assertFalse(result.allowed());
        assertEquals(expectedField, result.blockingField());
    }

    @Test
    void checkAlwaysAllowsInternalUse() {
        PolicyView policy = createPolicy(false, false, false, false);

        RuleOutcome result = policyRules.check(policy, IntendedUse.INTERNAL);

        assertTrue(result.allowed());
        assertEquals(null, result.blockingField());
    }

    @Test
    void obligationsAddsAttributionWhenRequired() {
        PolicyView policy = new PolicyView(
                new PolicyRef("test-policy", 1),
                true, true, true, true, true, false);

        AssetRef source = new AssetRef("model-A", 1);

        List<Obligation> result = policyRules.obligations(source, policy);

        assertEquals(1, result.size());
        assertEquals(ObligationType.ATTRIBUTION, result.get(0).type());
        assertEquals(source, result.get(0).source());
        assertEquals(policy.ref(), result.get(0).policy());
    }

    @Test
    void obligationsAddsShareAlikeWhenRequired() {
        PolicyView policy = new PolicyView(
                new PolicyRef("test-policy", 1),
                true, true, true, true, false, true);

        AssetRef source = new AssetRef("model-A", 1);

        List<Obligation> result = policyRules.obligations(source, policy);

        assertEquals(1, result.size());
        assertEquals(ObligationType.SHARE_ALIKE, result.get(0).type());
        assertEquals(source, result.get(0).source());
        assertEquals(policy.ref(), result.get(0).policy());
    }

    @Test
    void obligationsReturnsEmptyListWhenNoneAreRequired() {
        PolicyView policy = new PolicyView(
                new PolicyRef("test-policy", 1),
                true, true, true, true, false, false);

        AssetRef source = new AssetRef("model-A", 1);

        List<Obligation> result = policyRules.obligations(source, policy);

        assertTrue(result.isEmpty());
    }

    /**
     * AI-assisted: Tests that merging obligations removes duplicates
     * but keeps different sources.
     */
    @Test
    void mergeObligationsRemovesDuplicatesButKeepsDifferentSources() {
        PolicyRef policyRef = new PolicyRef("test-policy", 1);
        AssetRef sourceA = new AssetRef("model-A", 1);
        AssetRef sourceB = new AssetRef("model-B", 1);

        Obligation attributionA = new Obligation(
                ObligationType.ATTRIBUTION, sourceA, policyRef);
        Obligation duplicateAttributionA = new Obligation(
                ObligationType.ATTRIBUTION, sourceA, policyRef);
        Obligation attributionB = new Obligation(
                ObligationType.ATTRIBUTION, sourceB, policyRef);

        List<Obligation> result = policyRules.mergeObligations(
                List.of(attributionA, duplicateAttributionA, attributionB));

        assertEquals(2, result.size());
        assertEquals(sourceA, result.get(0).source());
        assertEquals(sourceB, result.get(1).source());
    }

    @Test
    void mergeObligationsSortsByAssetVersionAndType() {
        PolicyRef policyRef = new PolicyRef("test-policy", 1);

        Obligation modelB = new Obligation(
                ObligationType.ATTRIBUTION,
                new AssetRef("model-B", 1),
                policyRef);

        Obligation modelVersionTwo = new Obligation(
                ObligationType.ATTRIBUTION,
                new AssetRef("model-A", 2),
                policyRef);

        Obligation modelShareAlikeObligation = new Obligation(
                ObligationType.SHARE_ALIKE,
                new AssetRef("model-A", 1),
                policyRef);

        Obligation modelAttributionObligation = new Obligation(
                ObligationType.ATTRIBUTION,
                new AssetRef("model-A", 1),
                policyRef);

        List<Obligation> result = policyRules.mergeObligations(
                List.of(modelB, modelVersionTwo, modelShareAlikeObligation,
                        modelAttributionObligation));

        assertEquals(4, result.size());

        assertEquals("model-A", result.get(0).source().assetId());
        assertEquals(1, result.get(0).source().version());
        assertEquals(ObligationType.ATTRIBUTION, result.get(0).type());

        assertEquals("model-A", result.get(1).source().assetId());
        assertEquals(1, result.get(1).source().version());
        assertEquals(ObligationType.SHARE_ALIKE, result.get(1).type());

        assertEquals("model-A", result.get(2).source().assetId());
        assertEquals(2, result.get(2).source().version());

        assertEquals("model-B", result.get(3).source().assetId());
    }
}
