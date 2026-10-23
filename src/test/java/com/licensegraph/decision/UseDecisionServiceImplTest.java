
package com.licensegraph.decision;

import com.licensegraph.asset.AssetLookup;
import com.licensegraph.asset.AssetView;
import com.licensegraph.common.*;
import com.licensegraph.common.exception.NotFoundException;
import com.licensegraph.lineage.LineageQueryService;
import com.licensegraph.policy.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UseDecisionServiceImplTest {

    private AssetLookup assetLookup;
    private PolicyLookup policyLookup;
    private PolicyRules policyRules;
    private LineageQueryService lineageQueryService;
    private UseDecisionServiceImpl service;
    private DecisionAuditStore decisionAuditStore;

    @BeforeEach
    void setUp() {
        assetLookup = mock(AssetLookup.class);
        policyLookup = mock(PolicyLookup.class);
        policyRules = mock(PolicyRules.class);
        lineageQueryService = mock(LineageQueryService.class);
        decisionAuditStore = mock(DecisionAuditStore.class);

        service = new UseDecisionServiceImpl(
                assetLookup,
                policyLookup,
                policyRules,
                lineageQueryService,
                decisionAuditStore
        );
    }

    @Test
    void shouldDenyCommercialUseWhenDatasetLicenseBlocks() {

        // 1. Create our example assets.
        String clientId = "client-1";

        AssetRef datasetA = new AssetRef("Dataset-A", 1);
        AssetRef modelB = new AssetRef("Model-B", 1);
        AssetRef modelC = new AssetRef("Model-C", 1);

        PolicyRef licenseA = new PolicyRef("License-A", 1);
        PolicyRef licenseB = new PolicyRef("License-B", 1);
        PolicyRef licenseC = new PolicyRef("License-C", 1);

        // 2. Create the graph:
        // Dataset-A -> Model-B -> Model-C
        LineageGraph graph = new LineageGraph(
                modelC,
                Set.of(datasetA, modelB, modelC),
                List.of(
                        new LineageEdge(datasetA, modelB, null),
                        new LineageEdge(modelB, modelC, null)
                )
        );

        // 3. Pretend Module C returns this graph.
        when(lineageQueryService.findAncestors(clientId, modelC))
                .thenReturn(graph);

        // 4. Pretend Module B returns each asset's license.
        when(assetLookup.findAsset(clientId, datasetA))
                .thenReturn(Optional.of(
                        new AssetView(datasetA, null, licenseA)
                ));

        when(assetLookup.findAsset(clientId, modelB))
                .thenReturn(Optional.of(
                        new AssetView(modelB, null, licenseB)
                ));

        when(assetLookup.findAsset(clientId, modelC))
                .thenReturn(Optional.of(
                        new AssetView(modelC, null, licenseC)
                ));

        // 5. Create fake policy details.

        PolicyView policyA = new PolicyView(
                licenseA,
                false,  // commercialUse
                true,   // redistribution
                true,   // derivatives
                true,   // aiTraining
                true,   // attributionRequired
                false   // shareAlike
        );

        PolicyView policyB = new PolicyView(
                licenseB,
                true,
                true,
                true,
                true,
                false,
                false
        );

        PolicyView policyC = new PolicyView(
                licenseC,
                true,
                true,
                true,
                true,
                false,
                false
        );


        // Pretend Module A returns the policy details.
        when(policyLookup.findPolicy(clientId, licenseA))
                .thenReturn(Optional.of(policyA));

        when(policyLookup.findPolicy(clientId, licenseB))
                .thenReturn(Optional.of(policyB));

        when(policyLookup.findPolicy(clientId, licenseC))
                .thenReturn(Optional.of(policyC));

        // 6. Dataset-A blocks commercial use.
        when(policyRules.check(policyA, IntendedUse.COMMERCIAL))
                .thenReturn(new RuleOutcome(false, "commercialUse"));

        // Model-B and Model-C allow commercial use.
        when(policyRules.check(policyB, IntendedUse.COMMERCIAL))
                .thenReturn(new RuleOutcome(true, null));

        when(policyRules.check(policyC, IntendedUse.COMMERCIAL))
                .thenReturn(new RuleOutcome(true, null));

        // 7. For this test, there are no obligations.
        when(policyRules.obligations(any(), any()))
                .thenReturn(List.of());

        when(policyRules.mergeObligations(anyList()))
                .thenReturn(List.of());

        // 8. Call our real Module D method.
        UseDecisionResult result = service.evaluate(
                clientId,
                modelC,
                IntendedUse.COMMERCIAL
        );

        // 9. Check the final decision.
        assertEquals(Decision.DENY, result.decision());

        // 10. Check the blocking path.
        assertEquals(1, result.blockingPaths().size());

        BlockingPath blocking = result.blockingPaths().get(0);

        assertEquals(
                List.of(modelC, modelB, datasetA),
                blocking.path()
        );

        assertEquals(licenseA, blocking.blockingPolicy());
        assertEquals("commercialUse", blocking.blockingField());

        // 11. Check the evaluated policies.
        assertEquals(
                Set.of(licenseA, licenseB, licenseC),
                Set.copyOf(result.evaluatedPolicies())
        );

        // 12. Check the requested target and use.
        assertEquals(modelC, result.target());
        assertEquals(IntendedUse.COMMERCIAL, result.use());

        // 13. Confirm this test has no obligations.
        assertTrue(result.obligations().isEmpty());
    }

    @Test
    void shouldSaveDecisionWhenDecideAndRecordIsCalled() {

        String clientId = "client-1";
        AssetRef target = new AssetRef("Model-C", 1);

        // Create an empty lineage graph for this simple test.
        LineageGraph graph = new LineageGraph(
                target,
                Set.of(target),
                List.of()
        );

        when(lineageQueryService.findAncestors(clientId, target))
                .thenReturn(graph);

        PolicyRef licenseC = new PolicyRef("License-C", 1);

        when(assetLookup.findAsset(clientId, target))
                .thenReturn(Optional.of(
                        new AssetView(target, null, licenseC)
                ));

        PolicyView policyC = new PolicyView(
                licenseC,
                true, true, true, true, false, false
        );

        when(policyLookup.findPolicy(clientId, licenseC))
                .thenReturn(Optional.of(policyC));

        when(policyRules.check(policyC, IntendedUse.COMMERCIAL))
                .thenReturn(new RuleOutcome(true, null));

        when(policyRules.obligations(target, policyC))
                .thenReturn(List.of());

        when(policyRules.mergeObligations(anyList()))
                .thenReturn(List.of());

        // Simulate the audit store saving a decision.
        when(decisionAuditStore.save(eq(clientId), any(UseDecisionResult.class)))
                .thenAnswer(invocation -> {
                    UseDecisionResult result = invocation.getArgument(1);

                    return new UseDecisionRecord(
                            "decision-123",
                            java.time.Instant.parse("2026-10-20T12:00:00Z"),
                            result
                    );
                });

        // Call the real service.
        UseDecisionRecord record = service.decideAndRecord(
                clientId,
                target,
                IntendedUse.COMMERCIAL
        );

        // Check that it returned a saved record.
        assertEquals("decision-123", record.decisionId());
        assertEquals(Decision.ALLOW, record.result().decision());

        // Verify that the audit store's save method was called once.
        verify(decisionAuditStore, times(1))
                .save(eq(clientId), any(UseDecisionResult.class));
    }

    @Test
    void shouldAllowWhenAllPoliciesPermitCommercialUse() {

        String clientId = "client-1";

        AssetRef datasetA = new AssetRef("Dataset-A", 1);
        AssetRef modelB = new AssetRef("Model-B", 1);
        AssetRef modelC = new AssetRef("Model-C", 1);

        PolicyRef licenseA = new PolicyRef("License-A", 1);
        PolicyRef licenseB = new PolicyRef("License-B", 1);
        PolicyRef licenseC = new PolicyRef("License-C", 1);

        // Dataset-A -> Model-B -> Model-C
        LineageGraph graph = new LineageGraph(
                modelC,
                Set.of(datasetA, modelB, modelC),
                List.of(
                        new LineageEdge(datasetA, modelB, null),
                        new LineageEdge(modelB, modelC, null)
                )
        );

        when(lineageQueryService.findAncestors(clientId, modelC))
                .thenReturn(graph);

        // Configure assets
        when(assetLookup.findAsset(clientId, datasetA))
                .thenReturn(Optional.of(
                        new AssetView(datasetA, null, licenseA)
                ));

        when(assetLookup.findAsset(clientId, modelB))
                .thenReturn(Optional.of(
                        new AssetView(modelB, null, licenseB)
                ));

        when(assetLookup.findAsset(clientId, modelC))
                .thenReturn(Optional.of(
                        new AssetView(modelC, null, licenseC)
                ));

        // All three licenses allow commercial use
        PolicyView policyA = new PolicyView(
                licenseA, true, true, true, true, false, false
        );

        PolicyView policyB = new PolicyView(
                licenseB, true, true, true, true, false, false
        );

        PolicyView policyC = new PolicyView(
                licenseC, true, true, true, true, false, false
        );

        when(policyLookup.findPolicy(clientId, licenseA))
                .thenReturn(Optional.of(policyA));

        when(policyLookup.findPolicy(clientId, licenseB))
                .thenReturn(Optional.of(policyB));

        when(policyLookup.findPolicy(clientId, licenseC))
                .thenReturn(Optional.of(policyC));

        when(policyRules.check(any(PolicyView.class),
                eq(IntendedUse.COMMERCIAL)))
                .thenReturn(new RuleOutcome(true, null));

        when(policyRules.obligations(any(), any()))
                .thenReturn(List.of());

        when(policyRules.mergeObligations(anyList()))
                .thenReturn(List.of());

        // Execute
        UseDecisionResult result = service.evaluate(
                clientId,
                modelC,
                IntendedUse.COMMERCIAL
        );

        // Verify
        assertEquals(Decision.ALLOW, result.decision());

        assertTrue(result.blockingPaths().isEmpty());

        assertEquals(3, result.evaluatedPolicies().size());

        assertTrue(result.obligations().isEmpty());

        // evaluate() must not save anything
        verifyNoInteractions(decisionAuditStore);
    }

    @Test
    void shouldFindMultipleBlockingPaths() {

        String clientId = "client-1";

        AssetRef datasetA = new AssetRef("Dataset-A", 1);
        AssetRef modelB = new AssetRef("Model-B", 1);
        AssetRef modelC = new AssetRef("Model-C", 1);

        PolicyRef licenseA = new PolicyRef("License-A", 1);
        PolicyRef licenseB = new PolicyRef("License-B", 1);
        PolicyRef licenseC = new PolicyRef("License-C", 1);

        LineageGraph graph = new LineageGraph(
                modelC,
                Set.of(datasetA, modelB, modelC),
                List.of(
                        new LineageEdge(datasetA, modelB, null),
                        new LineageEdge(modelB, modelC, null)
                )
        );

        when(lineageQueryService.findAncestors(clientId, modelC))
                .thenReturn(graph);

        when(assetLookup.findAsset(clientId, datasetA))
                .thenReturn(Optional.of(
                        new AssetView(datasetA, null, licenseA)
                ));

        when(assetLookup.findAsset(clientId, modelB))
                .thenReturn(Optional.of(
                        new AssetView(modelB, null, licenseB)
                ));

        when(assetLookup.findAsset(clientId, modelC))
                .thenReturn(Optional.of(
                        new AssetView(modelC, null, licenseC)
                ));

        PolicyView policyA = new PolicyView(
                licenseA, false, true, true, true, false, false
        );

        PolicyView policyB = new PolicyView(
                licenseB, false, true, true, true, false, false
        );

        PolicyView policyC = new PolicyView(
                licenseC, true, true, true, true, false, false
        );

        when(policyLookup.findPolicy(clientId, licenseA))
                .thenReturn(Optional.of(policyA));

        when(policyLookup.findPolicy(clientId, licenseB))
                .thenReturn(Optional.of(policyB));

        when(policyLookup.findPolicy(clientId, licenseC))
                .thenReturn(Optional.of(policyC));

        when(policyRules.check(policyA, IntendedUse.COMMERCIAL))
                .thenReturn(new RuleOutcome(false, "commercialUse"));

        when(policyRules.check(policyB, IntendedUse.COMMERCIAL))
                .thenReturn(new RuleOutcome(false, "commercialUse"));

        when(policyRules.check(policyC, IntendedUse.COMMERCIAL))
                .thenReturn(new RuleOutcome(true, null));

        when(policyRules.obligations(any(), any()))
                .thenReturn(List.of());

        when(policyRules.mergeObligations(anyList()))
                .thenReturn(List.of());

        UseDecisionResult result = service.evaluate(
                clientId,
                modelC,
                IntendedUse.COMMERCIAL
        );

        assertEquals(Decision.DENY, result.decision());

        assertEquals(2, result.blockingPaths().size());

        assertTrue(result.blockingPaths().stream().anyMatch(
                path -> path.path().equals(
                        List.of(modelC, modelB, datasetA)
                ) && path.blockingPolicy().equals(licenseA)
        ));

        assertTrue(result.blockingPaths().stream().anyMatch(
                path -> path.path().equals(
                        List.of(modelC, modelB)
                ) && path.blockingPolicy().equals(licenseB)
        ));

        verifyNoInteractions(decisionAuditStore);
    }

    @Test
    void shouldIncludeAttributionObligationEvenWhenDenied() {

        String clientId = "client-1";

        AssetRef datasetA = new AssetRef("Dataset-A", 1);
        PolicyRef licenseA = new PolicyRef("License-A", 1);

        // A simple graph with one asset
        LineageGraph graph = new LineageGraph(
                datasetA,
                Set.of(datasetA),
                List.of()
        );

        when(lineageQueryService.findAncestors(clientId, datasetA))
                .thenReturn(graph);

        when(assetLookup.findAsset(clientId, datasetA))
                .thenReturn(Optional.of(
                        new AssetView(datasetA, null, licenseA)
                ));

        PolicyView policyA = new PolicyView(
                licenseA,
                false,  // commercialUse
                true,   // redistribution
                true,   // derivatives
                true,   // aiTraining
                true,   // attributionRequired
                false   // shareAlike
        );

        when(policyLookup.findPolicy(clientId, licenseA))
                .thenReturn(Optional.of(policyA));

        when(policyRules.check(policyA, IntendedUse.COMMERCIAL))
                .thenReturn(new RuleOutcome(false, "commercialUse"));

        // The license requires attribution.
        Obligation attribution = new Obligation(
                ObligationType.ATTRIBUTION,
                datasetA,
                licenseA
        );

        when(policyRules.obligations(datasetA, policyA))
                .thenReturn(List.of(attribution));

        when(policyRules.mergeObligations(List.of(attribution)))
                .thenReturn(List.of(attribution));

        // Evaluate the request.
        UseDecisionResult result = service.evaluate(
                clientId,
                datasetA,
                IntendedUse.COMMERCIAL
        );

        // Commercial use is denied.
        assertEquals(Decision.DENY, result.decision());

        // But the attribution obligation is still reported.
        assertEquals(1, result.obligations().size());

        assertEquals(
                attribution,
                result.obligations().get(0)
        );

        // The target itself is the blocking asset.
        assertEquals(
                List.of(datasetA),
                result.blockingPaths().get(0).path()
        );

        // evaluate() must not save anything.
        verifyNoInteractions(decisionAuditStore);
    }

    @Test
    void shouldThrowNotFoundWhenAssetIsMissing() {

        String clientId = "client-1";
        AssetRef target = new AssetRef("Missing-Model", 1);

        LineageGraph graph = new LineageGraph(
                target,
                Set.of(target),
                List.of()
        );

        when(lineageQueryService.findAncestors(clientId, target))
                .thenReturn(graph);

        when(assetLookup.findAsset(clientId, target))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> service.evaluate(
                        clientId,
                        target,
                        IntendedUse.COMMERCIAL
                )
        );

        verifyNoInteractions(decisionAuditStore);
    }

    @Test
    void shouldThrowNotFoundWhenPolicyIsMissing() {

        String clientId = "client-1";
        AssetRef target = new AssetRef("Model-C", 1);
        PolicyRef missingPolicy = new PolicyRef("Missing-License", 1);

        LineageGraph graph = new LineageGraph(
                target,
                Set.of(target),
                List.of()
        );

        when(lineageQueryService.findAncestors(clientId, target))
                .thenReturn(graph);

        when(assetLookup.findAsset(clientId, target))
                .thenReturn(Optional.of(
                        new AssetView(target, null, missingPolicy)
                ));

        when(policyLookup.findPolicy(clientId, missingPolicy))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> service.evaluate(
                        clientId,
                        target,
                        IntendedUse.COMMERCIAL
                )
        );

        verifyNoInteractions(decisionAuditStore);
    }





}
