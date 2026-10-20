package com.licensegraph.lineage;

import com.licensegraph.asset.PolicyConflict;
import com.licensegraph.common.LineageEdge;
import java.util.List;

/**
 * Result of recording a derivation batch (Section 10, Module C).
 */
public record DerivationResult(
        List<LineageEdge> created, List<PolicyConflict> conflicts, int ancestorCount) {
}
