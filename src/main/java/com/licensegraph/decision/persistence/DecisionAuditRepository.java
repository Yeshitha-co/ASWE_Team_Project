package com.licensegraph.decision.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for persisted decision audit records.
 *
 * <p>Provides database access for saving and retrieving
 * {@link DecisionAuditEntity} instances.
 *
 * <p>Custom query methods enforce client-scoped retrieval so that
 * decisions belonging to one client are not exposed to another.
 *
 * <p>The repository is package-private and is accessed through
 * the {@link JpaDecisionAuditStore} implementation rather than
 * directly by other modules.
 *
 * <p>AI-assisted: AI assistance was used to develop and document
 * client-scoped repository queries and refine code formatting.
 *
 */
interface DecisionAuditRepository
        extends JpaRepository<DecisionAuditEntity, String> {

    /**
     * Finds a saved decision by its unique ID and owning client.
     *
     * <p>Including the client ID ensures that a decision belonging
     * to another client is not returned.
     *
     * @param decisionId unique identifier of the saved decision
     * @param clientId authenticated client identifier
     * @return matching audit entity, or an empty Optional if absent
     */
    Optional<DecisionAuditEntity> findByDecisionIdAndClientId(
            String decisionId,
            String clientId
    );

    /**
     * Finds all recorded decisions for a specific client and asset version.
     *
     * <p>Spring Data JPA derives the database query from the method
     * name, filtering by client ID, asset ID, and asset version.
     *
     * @param clientId authenticated client identifier
     * @param assetId identifier of the evaluated asset
     * @param assetVersion version of the evaluated asset
     * @return matching audit entities
     */
    List<DecisionAuditEntity> findByClientIdAndAssetIdAndAssetVersion(
            String clientId,
            String assetId,
            int assetVersion
    );
}
