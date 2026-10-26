package com.licensegraph.decision;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.Decision;
import com.licensegraph.common.IntendedUse;
import com.licensegraph.common.Obligation;
import com.licensegraph.common.PolicyRef;
import java.util.List;

/**
 * ALLOW or DENY outcome for an intended use, with blockers and obligations
 * (Section 10, Module D).
 *
 * <p>Contains the target asset, intended use, final decision,
 * blocking paths, evaluated policies, and associated obligations.
 *
 * <p>AI-assisted: AI assistance was used to review and document
 * the decision result structure and improve code clarity.
 *
 * @param target asset being evaluated
 * @param use intended use being evaluated
 * @param decision final ALLOW or DENY outcome
 * @param blockingPaths paths explaining which policies block the use
 * @param evaluatedPolicies policy versions considered during evaluation
 * @param obligations license obligations associated with the evaluation
 */
public record UseDecisionResult(
        AssetRef target,
        IntendedUse use,
        Decision decision,
        List<BlockingPath> blockingPaths,
        List<PolicyRef> evaluatedPolicies,
        List<Obligation> obligations) {
}