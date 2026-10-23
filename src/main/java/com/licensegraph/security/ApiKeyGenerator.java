package com.licensegraph.security;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * Creates API keys from a cryptographic random source (E2).
 */
@Component
public class ApiKeyGenerator {

    private static final int KEY_BYTES = 32;

    private final SecureRandom random = new SecureRandom();

    /**
     * Returns a URL-safe key. The caller shows it once; only its hash is stored.
     */
    public String generate() {
        byte[] bytes = new byte[KEY_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
