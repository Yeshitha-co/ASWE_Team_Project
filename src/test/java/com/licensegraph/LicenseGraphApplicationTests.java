package com.licensegraph;

import com.licensegraph.security.ClientContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies the Spring Boot application context starts.
 */
@SpringBootTest
class LicenseGraphApplicationTests {

    @MockitoBean
    private ClientContext clientContext;

    @Test
    void contextLoads() {
    }
}