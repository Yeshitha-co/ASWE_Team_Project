package com.licensegraph.lineage;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.LineageGraph;
import com.licensegraph.common.exception.NotFoundException;

/**
 * Ancestry and descendant queries over the derivation graph (Section 10, Module C).
 *
 * <p>Implemented by Module C. Used by D. Missing targets throw {@link NotFoundException}.
 */
public interface LineageQueryService {

    LineageGraph findAncestors(String clientId, AssetRef target);

    LineageGraph findDescendants(String clientId, AssetRef source);
}
