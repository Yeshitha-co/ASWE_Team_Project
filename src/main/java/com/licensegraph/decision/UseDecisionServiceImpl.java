
package com.licensegraph.decision;

import com.licensegraph.asset.AssetLookup;
import com.licensegraph.asset.AssetView;
import com.licensegraph.common.AssetRef;
import com.licensegraph.common.Decision;
import com.licensegraph.common.IntendedUse;
import com.licensegraph.common.LineageGraph;
import com.licensegraph.common.Obligation;
import com.licensegraph.common.PolicyRef;
import com.licensegraph.common.exception.NotFoundException;
import com.licensegraph.lineage.LineageQueryService;
import com.licensegraph.policy.PolicyLookup;
import com.licensegraph.policy.PolicyRules;
import com.licensegraph.policy.PolicyView;
import com.licensegraph.policy.RuleOutcome;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Implements lineage-aware use decision evaluation for Module D.
 *
 * <p>Coordinates asset lookup (Module B), policy evaluation (Module A),
 * and lineage queries (Module C) to determine whether a requested use
 * is permitted for an asset and its ancestors.
 *
 * <p>The service combines policy outcomes, identifies blocking paths,
 * and collects applicable license obligations. Evaluation and audit
 * persistence are kept separate so decisions can be evaluated
 * without modifying stored data.
 *
 * <p>AI-assisted: AI assistance was used to review the decision
 * evaluation implementation, refine documentation, organize imports,
 * and resolve Checkstyle warnings.
 *
 */

@Service
public class UseDecisionServiceImpl implements UseDecisionService {

    private final AssetLookup assetLookup;
    private final PolicyLookup policyLookup;
    private final PolicyRules policyRules;
    private final LineageQueryService lineageQueryService;

    private final PolicyEvaluation policyEvaluation;
    private final BlockingPathBuilder blockingPathBuilder;
    private final DecisionAuditStore decisionAuditStore;

    /**
     * Creates the decision service with its required collaborators.
     *
     * @param assetLookup retrieves stored asset versions
     * @param policyLookup retrieves the policies attached to assets
     * @param policyRules evaluates permissions and obligations
     * @param lineageQueryService retrieves an asset's ancestry graph
     * @param decisionAuditStore persists completed decision records
     */
    public UseDecisionServiceImpl(
            AssetLookup assetLookup,
            PolicyLookup policyLookup,
            PolicyRules policyRules,
            LineageQueryService lineageQueryService,
            DecisionAuditStore decisionAuditStore) {

        this.assetLookup = assetLookup;
        this.policyLookup = policyLookup;
        this.policyRules = policyRules;
        this.lineageQueryService = lineageQueryService;
        this.decisionAuditStore = decisionAuditStore;

        this.policyEvaluation = new PolicyEvaluation();
        this.blockingPathBuilder = new BlockingPathBuilder();
    }

    /**
     * Evaluates an intended use across the target asset's lineage.
     *
     * <p>Checks the policy associated with each asset in the ancestry
     * graph. Any denying policy contributes a blocking path from the
     * target to the asset responsible for the restriction.
     *
     * <p>All policy outcomes are combined into one decision, while
     * obligations are collected and merged separately. This method
     * does not save the resulting decision to the audit database.
     *
     * @param clientId authenticated client identifier
     * @param target asset version for which permission is requested
     * @param use intended use being evaluated
     * @return combined decision, blocking paths, evaluated policies,
     *         and applicable obligations
     * @throws NotFoundException if an asset or its policy cannot be found
     */
    @Override
    public UseDecisionResult evaluate(
            String clientId,
            AssetRef target,
            IntendedUse use) {

        // Retrieve the lineage and include the target even if not
        // explicitly present in the returned ancestor nodes.
        LineageGraph graph =
                lineageQueryService.findAncestors(clientId, target);

        Set<AssetRef> assets = new HashSet<>(graph.nodes());
        assets.add(target);

        // Collect results from every asset before constructing
        // the final decision response.
        List<RuleOutcome> outcomes = new ArrayList<>();
        List<PolicyRef> evaluatedPolicies = new ArrayList<>();
        List<Obligation> allObligations = new ArrayList<>();
        List<BlockingPath> blockingPaths = new ArrayList<>();

        for (AssetRef asset : assets) {

            // Resolve the asset and its attached policy within
            // the authenticated client's scope.
            AssetView assetView =
                    assetLookup.findAsset(clientId, asset)
                            .orElseThrow(() ->
                                    NotFoundException.asset(asset.toString())
                            );

            PolicyRef policyRef = assetView.policy();
            evaluatedPolicies.add(policyRef);

            PolicyView policyView =
                    policyLookup.findPolicy(clientId, policyRef)
                            .orElseThrow(() ->
                                    NotFoundException.policy(policyRef.toString())
                            );

            // Check whether this policy permits the requested use.
            RuleOutcome outcome = policyRules.check(policyView, use);
            outcomes.add(outcome);

            // Explain each denial using a shortest lineage path
            // from the target to the blocking asset.
            if (!outcome.allowed()) {
                BlockingPath blockingPath = blockingPathBuilder.build(
                        graph,
                        target,
                        asset,
                        policyRef,
                        outcome.blockingField()
                );

                blockingPaths.add(blockingPath);
            }

            // Report license obligations independently of whether
            // the final permission decision will be ALLOW or DENY.
            List<Obligation> obligations =
                    policyRules.obligations(asset, policyView);

            allObligations.addAll(obligations);
        }

        // A single denying policy is sufficient for a DENY result.
        Decision finalDecision = policyEvaluation.evaluate(outcomes);

        // Merge duplicate obligations according to Module A rules.
        List<Obligation> mergedObligations =
                policyRules.mergeObligations(allObligations);

        return new UseDecisionResult(
                target,
                use,
                finalDecision,
                blockingPaths,
                evaluatedPolicies,
                mergedObligations
        );
    }

    /**
     * Evaluates a requested use and persists the completed result.
     *
     * <p>Delegates the decision calculation to {@link #evaluate}
     * and stores the result using {@link DecisionAuditStore}.
     * Unlike evaluate(), this method creates an audit record.
     *
     * @param clientId authenticated client identifier
     * @param target asset version being evaluated
     * @param use intended use being evaluated
     * @return saved decision record containing its ID, timestamp,
     *         and evaluation result
     */
    @Override
    public UseDecisionRecord decideAndRecord(
            String clientId,
            AssetRef target,
            IntendedUse use) {

        UseDecisionResult result = evaluate(clientId, target, use);

        return decisionAuditStore.save(clientId, result);
    }
}
