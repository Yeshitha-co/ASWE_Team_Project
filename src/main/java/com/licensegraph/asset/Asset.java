package com.licensegraph.asset;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.AssetType;
import com.licensegraph.common.PolicyRef;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.Immutable;

/**
 * One stored asset version, owned by a single client (Module B, task B1).
 *
 * <p>Assets are insert-only: there are no setters and Hibernate never issues updates for this
 * entity. Linking an asset to a different policy means registering a new asset version. The
 * pair (assetId, version) is unique per client, so two clients may each own a {@code dataset-A:v1}.
 *
 * <p>AI-assisted (Claude): entity and mapping drafted with Claude, reviewed by the Module B owner.
 */
@Entity
@Immutable
@Table(
        name = "assets",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_assets_client_asset_version",
                columnNames = {"client_id", "asset_id", "version"}))
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false, updatable = false, length = 64)
    private String clientId;

    @Column(name = "asset_id", nullable = false, updatable = false)
    private String assetId;

    @Column(name = "version", nullable = false, updatable = false)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, length = 32)
    private AssetType type;

    @Column(name = "policy_id", nullable = false, updatable = false)
    private String policyId;

    @Column(name = "policy_version", nullable = false, updatable = false)
    private int policyVersion;

    @Column(name = "metadata", updatable = false, length = 2000)
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Required by JPA; not for application code. */
    protected Asset() {
    }

    /**
     * Creates a new asset version.
     *
     * @param clientId owning client, taken from {@code ClientContext}, never from the request body
     * @param ref asset id and version
     * @param type asset type
     * @param policy exact policy version this asset version carries
     * @param metadata optional free-text name or source reference; may be {@code null}
     */
    public Asset(String clientId, AssetRef ref, AssetType type, PolicyRef policy, String metadata) {
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.assetId = Objects.requireNonNull(ref, "ref").assetId();
        this.version = ref.version();
        this.type = Objects.requireNonNull(type, "type");
        this.policyId = Objects.requireNonNull(policy, "policy").policyId();
        this.policyVersion = policy.version();
        this.metadata = metadata;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getClientId() {
        return clientId;
    }

    public AssetRef getRef() {
        return new AssetRef(assetId, version);
    }

    public AssetType getType() {
        return type;
    }

    public PolicyRef getPolicy() {
        return new PolicyRef(policyId, policyVersion);
    }

    public String getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Converts to the read model other modules see through {@code AssetLookup}. */
    public AssetView toView() {
        return new AssetView(getRef(), type, getPolicy());
    }
}
