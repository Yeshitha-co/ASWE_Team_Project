package com.licensegraph.asset;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.DerivationInput;
import com.licensegraph.common.LineageGraph;
import java.util.List;
import java.util.Map;

/**
 * Checks a new derivation against each input's upstream graph (Section 10, Module B).
 *
 * <p>C supplies each input's upstream graph (the input plus its ancestors); B never calls C.
 */
public interface DerivationConflictChecker {

    List<PolicyConflict> check(
            String clientId,
            List<DerivationInput> inputs,
            Map<AssetRef, LineageGraph> upstreamByInput);
}
