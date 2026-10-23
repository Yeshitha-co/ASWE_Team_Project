package com.licensegraph.lineage;

import com.licensegraph.asset.PolicyConflict;
import com.licensegraph.common.AssetRef;
import com.licensegraph.common.PolicyRef;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * One upstream policy conflict stored on a derivation edge (C1, C5).
 *
 * <p>Conflicts are reported and kept for history. They never block a derivation.
 */

@Embeddable
public class EdgeConflict {

    @Column(name = "source_asset_id", nullable = false)
    private String sourceAssetId;

    @Column(name = "source_version", nullable = false)
    private int sourceVersion;

    @Column(name = "policy_id", nullable = false)
    private String policyId;

    @Column(name = "policy_version", nullable = false)
    private int policyVersion;

    @Column(name = "conflict_field", nullable = false)
    private String field;

    /**
     * Required by JPA.
     */
    protected EdgeConflict() {
    }

    private EdgeConflict(AssetRef source, PolicyRef policy, String field) {
        this.sourceAssetId = source.assetId();
        this.sourceVersion = source.version();
        this.policyId = policy.policyId();
        this.policyVersion = policy.version();
        this.field = field;
    }

    /**
     * Converts Module B's conflict record into a storable row.
     */
    public static EdgeConflict from(PolicyConflict conflict) {
        return new EdgeConflict(conflict.source(), conflict.policy(), conflict.field());
    }

    /**
     * Converts the stored row back into Module B's shared record.
     */
    public PolicyConflict toPolicyConflict() {
        return new PolicyConflict(
                new AssetRef(sourceAssetId, sourceVersion),
                new PolicyRef(policyId, policyVersion),
                field);
    }
}