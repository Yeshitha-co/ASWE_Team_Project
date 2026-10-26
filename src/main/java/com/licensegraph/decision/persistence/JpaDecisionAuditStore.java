
package com.licensegraph.decision.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.licensegraph.common.AssetRef;
import com.licensegraph.decision.DecisionAuditStore;
import com.licensegraph.decision.UseDecisionRecord;
import com.licensegraph.decision.UseDecisionResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
/**
 * JPA-backed implementation of the decision audit store for Module D.
 *
 * <p>Persists evaluated use decisions as append-only audit records.
 * Each saved decision receives a unique identifier and evaluation
 * timestamp. The complete evaluation result is serialized as JSON
 * so that blocking paths, evaluated policies, and obligations can
 * be reconstructed when the record is retrieved.
 *
 * <p>All retrieval operations are scoped to the requesting client
 * to prevent access to another client's decision history.
 *
 * <p>AI-assisted: AI assistance was used to review the audit
 * persistence implementation, improve documentation, and resolve
 *
 */

@Repository
public class JpaDecisionAuditStore implements DecisionAuditStore {

    private final DecisionAuditRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Creates an audit store backed by a JPA repository.
     *
     * @param repository database access for audit entities
     * @param objectMapper converts decision results to and from JSON
     */
    public JpaDecisionAuditStore(
            DecisionAuditRepository repository,
            ObjectMapper objectMapper) {

        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates and persists a new audit record for an evaluated decision.
     *
     * <p>Generates a unique decision ID and evaluation timestamp,
     * serializes the complete result, and stores it in the database.
     * Existing records are not modified by this method.
     *
     * @param clientId authenticated client identifier
     * @param result completed evaluation result
     * @return saved record containing its ID, timestamp, and result
     * @throws IllegalStateException if result serialization fails
     */
    @Override
    public UseDecisionRecord save(
            String clientId,
            UseDecisionResult result) {

        // Create identifiers for a new audit event.
        String decisionId = UUID.randomUUID().toString();
        Instant evaluatedAt = Instant.now();

        // Preserve the complete evaluation result as JSON.
        String resultJson = toJson(result);

        DecisionAuditEntity entity = new DecisionAuditEntity(
                decisionId,
                clientId,
                result.target().assetId(),
                result.target().version(),
                evaluatedAt,
                resultJson
        );

        repository.save(entity);

        return new UseDecisionRecord(
                decisionId,
                evaluatedAt,
                result
        );
    }

    /**
     * Retrieves a saved decision by its ID within a client's scope.
     *
     * @param clientId authenticated client identifier
     * @param decisionId unique identifier of the saved decision
     * @return the matching record, or an empty Optional if not found
     */
    @Override
    public Optional<UseDecisionRecord> findById(
            String clientId,
            String decisionId) {

        // Include clientId in the lookup to enforce client isolation.
        return repository
                .findByDecisionIdAndClientId(decisionId, clientId)
                .map(this::toRecord);
    }

    /**
     * Retrieves the audit history for a specific asset version.
     *
     * <p>Only decisions belonging to the supplied client and exact
     * asset version are included.
     *
     * @param clientId authenticated client identifier
     * @param target asset version whose history is requested
     * @return matching audit records
     */
    @Override
    public List<UseDecisionRecord> findByTarget(
            String clientId,
            AssetRef target) {

        return repository
                .findByClientIdAndAssetIdAndAssetVersion(
                        clientId,
                        target.assetId(),
                        target.version()
                )
                .stream()
                .map(this::toRecord)
                .toList();
    }

    /**
     * Serializes an evaluation result for database storage.
     *
     * @param result decision result to serialize
     * @return JSON representation of the result
     * @throws IllegalStateException if Jackson cannot serialize the result
     */
    private String toJson(UseDecisionResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize decision result", e
            );
        }
    }

    /**
     * Reconstructs a decision record from a stored database entity.
     *
     * @param entity persisted audit entity containing the JSON result
     * @return decision record reconstructed from database fields
     * @throws IllegalStateException if Jackson cannot deserialize the result
     */
    private UseDecisionRecord toRecord(DecisionAuditEntity entity) {
        try {
            UseDecisionResult result = objectMapper.readValue(
                    entity.getResultJson(),
                    UseDecisionResult.class
            );

            return new UseDecisionRecord(
                    entity.getDecisionId(),
                    entity.getEvaluatedAt(),
                    result
            );

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to deserialize decision result", e
            );
        }
    }
}
