
package com.licensegraph.decision.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA entity representing a persisted use decision audit record.
 *
 * <p>Each entity stores the unique decision identifier, the client
 * that requested the evaluation, the target asset and its version,
 * and the time the decision was evaluated.
 *
 * <p>The complete evaluation result is stored as JSON to preserve
 * the decision, blocking lineage paths, evaluated policies, and
 * associated obligations.
 *
 * <p>This entity is used by the Module D audit persistence layer.
 * Database access is handled through DecisionAuditRepository.
 *
 */
@Entity
@Table(name = "decision_audit")
public class DecisionAuditEntity {

    /** Unique identifier of the saved decision. */
    @Id
    @Column(name = "decision_id", nullable = false)
    private String decisionId;

    /** Identifier of the client that owns this audit record. */
    @Column(name = "client_id", nullable = false)
    private String clientId;

    /** Identifier of the asset whose use was evaluated. */
    @Column(name = "asset_id", nullable = false)
    private String assetId;

    /** Version of the asset whose use was evaluated. */
    @Column(name = "asset_version", nullable = false)
    private int assetVersion;

    /** Timestamp when the decision was evaluated. */
    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    /** Serialized JSON containing the complete evaluation result. */
    @Column(name = "result_json", nullable = false, columnDefinition = "text")
    private String resultJson;

    /**
     * No-argument constructor required by JPA for entity creation.
     */
    protected DecisionAuditEntity() {
        // Required by JPA.
    }

    /**
     * Creates an audit entity from a completed use decision.
     *
     * @param decisionId unique identifier of the decision
     * @param clientId identifier of the client that owns the record
     * @param assetId identifier of the evaluated asset
     * @param assetVersion version of the evaluated asset
     * @param evaluatedAt timestamp of the evaluation
     * @param resultJson serialized evaluation result
     */
    public DecisionAuditEntity(
            String decisionId,
            String clientId,
            String assetId,
            int assetVersion,
            Instant evaluatedAt,
            String resultJson) {

        this.decisionId = decisionId;
        this.clientId = clientId;
        this.assetId = assetId;
        this.assetVersion = assetVersion;
        this.evaluatedAt = evaluatedAt;
        this.resultJson = resultJson;
    }

    public String getDecisionId() {
        return decisionId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getAssetId() {
        return assetId;
    }

    public int getAssetVersion() {
        return assetVersion;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public String getResultJson() {
        return resultJson;
    }
}
