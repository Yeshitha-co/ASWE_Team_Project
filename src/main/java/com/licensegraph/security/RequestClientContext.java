package com.licensegraph.security;

import com.licensegraph.common.exception.UnauthorizedException;
import org.springframework.stereotype.Component;

/**
 * AI-assisted: Client identity for the current request.
 *
 * <p>{@link ClientContext} bean set by the API-key filter for the current request (E3).
 */
@Component
public class RequestClientContext implements ClientContext {

    @Override
    public String currentClientId() {
        String clientId = ClientIdHolder.get();
        if (clientId == null || clientId.isBlank()) {
            throw UnauthorizedException.missingApiKey();
        }
        return clientId;
    }
}
