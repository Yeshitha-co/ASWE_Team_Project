package com.licensegraph.decision;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.Decision;
import com.licensegraph.common.IntendedUse;
import com.licensegraph.common.Obligation;
import com.licensegraph.common.PolicyRef;
import java.util.List;

/**
 * ALLOW or DENY outcome for an intended use, with blockers and obligations (Section 10, Module D).
 */
public record UseDecisionResult(
        AssetRef target,
        IntendedUse use,
        Decision decision,
        List<BlockingPath> blockingPaths,
        List<PolicyRef> evaluatedPolicies,
        List<Obligation> obligations) {
}
