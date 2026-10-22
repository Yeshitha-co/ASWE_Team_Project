
package com.licensegraph.policy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "license_policies",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_policy_client_policy_version",
                        columnNames = {"client_id", "policy_id", "version"}
                )
        }
)
public class LicensePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;

    @Column(name = "policy_id", nullable = false, length = 100)
    private String policyId;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "commercial_use", nullable = false)
    private boolean commercialUse;

    @Column(name = "redistribution", nullable = false)
    private boolean redistribution;

    @Column(name = "derivatives", nullable = false)
    private boolean derivatives;

    @Column(name = "ai_training", nullable = false)
    private boolean aiTraining;

    @Column(name = "attribution_required", nullable = false)
    private boolean attributionRequired;

    @Column(name = "share_alike", nullable = false)
    private boolean shareAlike;

    protected LicensePolicy() {
        // Required by JPA.
    }

    public LicensePolicy(
            String clientId,
            String policyId,
            int version,
            boolean commercialUse,
            boolean redistribution,
            boolean derivatives,
            boolean aiTraining,
            boolean attributionRequired,
            boolean shareAlike) {
        this.clientId = clientId;
        this.policyId = policyId;
        this.version = version;
        this.commercialUse = commercialUse;
        this.redistribution = redistribution;
        this.derivatives = derivatives;
        this.aiTraining = aiTraining;
        this.attributionRequired = attributionRequired;
        this.shareAlike = shareAlike;
    }

    public Long getId() {
        return id;
    }

    public String getClientId() {
        return clientId;
    }

    public String getPolicyId() {
        return policyId;
    }

    public int getVersion() {
        return version;
    }

    public boolean isCommercialUse() {
        return commercialUse;
    }

    public boolean isRedistribution() {
        return redistribution;
    }

    public boolean isDerivatives() {
        return derivatives;
    }

    public boolean isAiTraining() {
        return aiTraining;
    }

    public boolean isAttributionRequired() {
        return attributionRequired;
    }

    public boolean isShareAlike() {
        return shareAlike;
    }
}
