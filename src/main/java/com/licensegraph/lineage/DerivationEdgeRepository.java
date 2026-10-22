package com.licensegraph.lineage;

import java.util.List;

import com.licensegraph.common.RelationType;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for derivation edges (C1). Used only inside Module C; other modules go through
 * {@link LineageQueryService}.
 */

public interface DerivationEdgeRepository extends JpaRepository<DerivationEdge, Long> {

    /** Edges pointing INTO an asset, i.e. its direct parents. Used for ancestor walks. */
    List<DerivationEdge> findByClientIdAndOutputAssetIdAndOutputVersion(
            String clientId, String outputAssetId, int outputVersion);

    /** Edges going OUT of an asset, i.e. its direct children. Used for descendant walks. */
    List<DerivationEdge> findByClientIdAndInputAssetIdAndInputVersion(
            String clientId, String inputAssetId, int inputVersion);

    /** True when this exact edge is already stored. Used for 409 DUPLICATE_EDGE. */
    boolean existsByClientIdAndInputAssetIdAndInputVersionAndOutputAssetIdAndOutputVersionAndRelation(
            String clientId, String inputAssetId, int inputVersion,
            String outputAssetId, int outputVersion, RelationType relation);
}