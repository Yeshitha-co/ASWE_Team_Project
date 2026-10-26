package com.licensegraph.lineage;

import com.licensegraph.common.RelationType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * AI-assisted: Data access for derivation edges (C1). Used only inside Module C; other modules go
 * through
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
    @Query("""
            select count(e) > 0 from DerivationEdge e
            where e.clientId = :clientId
              and e.inputAssetId = :inputAssetId and e.inputVersion = :inputVersion
              and e.outputAssetId = :outputAssetId and e.outputVersion = :outputVersion
              and e.relation = :relation
            """)
    boolean existsEdge(@Param("clientId") String clientId,
                       @Param("inputAssetId") String inputAssetId,
                       @Param("inputVersion") int inputVersion,
                       @Param("outputAssetId") String outputAssetId,
                       @Param("outputVersion") int outputVersion,
                       @Param("relation") RelationType relation);
}