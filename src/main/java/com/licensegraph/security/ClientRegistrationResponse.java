package com.licensegraph.security;

/**
 * 201 body for {@code POST /api/clients}. {@code apiKey} is returned only from this
 * response.
 */
public record ClientRegistrationResponse(String clientId, String apiKey) {
}
