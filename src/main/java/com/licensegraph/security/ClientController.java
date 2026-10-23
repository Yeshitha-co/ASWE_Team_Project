package com.licensegraph.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /api/clients} (E2). No API key is required, and no {@code clientId} is read from the body.
 */
@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientRegistrationService registrations;

    public ClientController(ClientRegistrationService registrations) {
        this.registrations = registrations;
    }

    /**
     * Registers a client and returns the raw API key once.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientRegistrationResponse register() {
        return registrations.register();
    }
}
