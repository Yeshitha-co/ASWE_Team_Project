package com.licensegraph.security;

/**
 * Request-thread storage for the authenticated client id. Cleared at the end of
 * the filter.
 */
final class ClientIdHolder {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private ClientIdHolder() {
    }

    static void set(String clientId) {
        CURRENT.set(clientId);
    }

    static String get() {
        return CURRENT.get();
    }

    static void clear() {
        CURRENT.remove();
    }
}
