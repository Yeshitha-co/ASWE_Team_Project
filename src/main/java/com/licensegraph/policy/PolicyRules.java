package com.licensegraph.policy;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.IntendedUse;
import com.licensegraph.common.Obligation;
import java.util.List;

/**
 * Pure policy evaluation with no database access (Section 10, Module A).
 *
 * <p>Implemented by Module A. Used by D. {@code mergeObligations} keeps one duty per type per
 * source and sorts by asset, then type.
 */
public interface PolicyRules {

    RuleOutcome check(PolicyView policy, IntendedUse use);

    List<Obligation> obligations(AssetRef source, PolicyView policy);

    List<Obligation> mergeObligations(List<Obligation> all);
}
