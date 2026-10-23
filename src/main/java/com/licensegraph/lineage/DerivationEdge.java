package com.licensegraph.lineage;

import com.licensegraph.asset.PolicyConflict;
import com.licensegraph.common.AssetRef;
import com.licensegraph.common.LineageEdge;
import com.licensegraph.common.RelationType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One stored derivation edge, directed from input to output and owned by one client (C1).
 *
 * <p>Edges are insert-only: there are no setters, and the only change after construction is
 * attaching the conflicts found for this edge inside the same transaction.
 */

@Entity
@Table(
        name = "derivation_edge",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_derivation_edge",
                columnNames = {"client_id", "input_asset_id", "input_version",
                        "output_asset_id", "output_version", "relation"}),
        indexes = {
                @Index(name = "ix_edge_by_output", columnList = "client_id, output_asset_id, "
                       + "output_version"),
                @Index(name = "ix_edge_by_input", columnList = "client_id, input_asset_id, "
                       + "input_version")
        })
public class DerivationEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "input_asset_id", nullable = false)
    private String inputAssetId;

    @Column(name = "input_version", nullable = false)
    private int inputVersion;

    @Column(name = "output_asset_id", nullable = false)
    private String outputAssetId;

    @Column(name = "output_version", nullable = false)
    private int outputVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation", nullable = false, length = 32)
    private RelationType relation;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ElementCollection
    @CollectionTable(name = "derivation_edge_conflict", joinColumns = @JoinColumn(name = "edge_id"))
    private List<EdgeConflict> conflicts = new ArrayList<>();

    /**
     * Required by JPA.
     */
    protected DerivationEdge() {
    }

    /**
     * Creates a new edge for one input of a derivation request.
     */
    public DerivationEdge(String clientId, AssetRef input, AssetRef output,
                          RelationType relation, Instant createdAt) {
        this.clientId = clientId;
        this.inputAssetId = input.assetId();
        this.inputVersion = input.version();
        this.outputAssetId = output.assetId();
        this.outputVersion = output.version();
        this.relation = relation;
        this.createdAt = createdAt;
    }

    /**
     * Attaches the conflicts Module B found for this edge's input (C5).
     */
    public void recordConflicts(List<PolicyConflict> found) {
        for (PolicyConflict conflict : found) {
            conflicts.add(EdgeConflict.from(conflict));
        }
    }

    public Long getId() {
        return id;
    }

    public String getClientId() {
        return clientId;
    }

    public AssetRef input() {
        return new AssetRef(inputAssetId, inputVersion);
    }

    public AssetRef output() {
        return new AssetRef(outputAssetId, outputVersion);
    }

    public RelationType getRelation() {
        return relation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<PolicyConflict> getConflicts() {
        return conflicts.stream().map(EdgeConflict::toPolicyConflict).toList();
    }

    /**
     * Converts this row into the shared record other modules read.
     */
    public LineageEdge toLineageEdge() {
        return new LineageEdge(input(), output(), relation);
    }

    /**
     * Unmodifiable view, mainly for tests.
     */
    List<EdgeConflict> storedConflicts() {
        return Collections.unmodifiableList(conflicts);
    }
}