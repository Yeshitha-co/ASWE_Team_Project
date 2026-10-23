package com.licensegraph.security;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Clients keyed by the hash of their API key. There is no lookup by the raw key.
 */
public interface ApiClientRepository extends JpaRepository<ApiClient, String> {

    Optional<ApiClient> findByKeyHash(String keyHash);
}
