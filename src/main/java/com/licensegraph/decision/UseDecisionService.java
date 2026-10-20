package com.licensegraph.decision;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.IntendedUse;

/**
 * Evaluates intended use across an asset's lineage (Section 10, Module D).
 *
 * <p>{@code evaluate} is pure (no writes) so Iteration 2 impact analysis can re-run it.
 * {@code decideAndRecord} evaluates and stores the audit row.
 */
public interface UseDecisionService {

    UseDecisionResult evaluate(String clientId, AssetRef target, IntendedUse use);

    UseDecisionRecord decideAndRecord(String clientId, AssetRef target, IntendedUse use);
}
