package com.licensegraph.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Checks the stored key digest and that generated keys come from a random source.
 */
class ApiKeyHasherTest {

    @Test
    void sha256HexMatchesKnownDigest() {
        assertThat(ApiKeyHasher.sha256Hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void generatedKeysDifferAndAreNotTheHash() {
        ApiKeyGenerator generator = new ApiKeyGenerator();
        String first = generator.generate();
        String second = generator.generate();

        assertThat(first).isNotEqualTo(second);
        assertThat(first).hasSize(43);
        assertThat(ApiKeyHasher.sha256Hex(first)).isNotEqualTo(first).hasSize(64);
    }
}
