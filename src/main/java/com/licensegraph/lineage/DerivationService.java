package com.licensegraph.lineage;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.DerivationInput;
import com.licensegraph.common.exception.ConflictException;
import java.util.List;

/**
 * Records a derivation of one output from one or more inputs (Section 10, Module C).
 *
 * <p>Throws {@link ConflictException} for {@code CYCLE_DETECTED} or {@code DUPLICATE_EDGE}.
 * Cycle check and insert run in one transaction; the whole batch rolls back on failure.
 */
public interface DerivationService {

    DerivationResult recordDerivation(String clientId, AssetRef output, List<DerivationInput> inputs);
}
