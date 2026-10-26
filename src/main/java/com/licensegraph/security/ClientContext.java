package com.licensegraph.security;

/**
 * Exposes the authenticated caller. Set by the X-API-Key filter (Section 10,
 * Module E).
 *
 * <p>Controllers read this; every service method takes {@code clientId} explicitly so Mockito
 * tests stay free of security setup. No endpoint trusts a {@code clientId} sent in the body.
 *
 * <p>The filter that sets this value is task E3.
 */
public interface ClientContext {

    String currentClientId();
}
