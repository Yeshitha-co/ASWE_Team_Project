package com.licensegraph.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Registered caller. Only the SHA-256 hash of the API key is stored.
 */
@Entity
@Table(
        name = "api_clients",
        uniqueConstraints = @UniqueConstraint(name = "uk_api_clients_key_hash", columnNames = "key_hash"))
public class ApiClient {

    @Id
    @Column(name = "client_id", nullable = false, length = 36)
    private String clientId;

    @Column(name = "key_hash", nullable = false, length = 64)
    private String keyHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ApiClient() {
        // Required by JPA.
    }

    /**
     * Creates a client row. {@code keyHash} is the hex SHA-256 of the raw key, never the key itself.
     */
    public ApiClient(String clientId, String keyHash, Instant createdAt) {
        this.clientId = clientId;
        this.keyHash = keyHash;
        this.createdAt = createdAt;
    }

    public String getClientId() {
        return clientId;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
