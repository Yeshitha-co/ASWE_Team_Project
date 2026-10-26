package com.licensegraph.security;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI-assisted: Key generation, hashing, and client registration.
 *
 * <p>Registers a client and returns its API key once (E2).
 */
@Service
public class ClientRegistrationService {

    private final ApiClientRepository clients;
    private final ApiKeyGenerator keys;

    public ClientRegistrationService(ApiClientRepository clients, ApiKeyGenerator keys) {
        this.clients = clients;
        this.keys = keys;
    }

    /**
     * Creates a new client. The raw key is not written to the database.
     */
    @Transactional
    public ClientRegistrationResponse register() {
        String apiKey = keys.generate();
        String clientId = UUID.randomUUID().toString();
        clients.save(new ApiClient(clientId, ApiKeyHasher.sha256Hex(apiKey), Instant.now()));
        return new ClientRegistrationResponse(clientId, apiKey);
    }
}
