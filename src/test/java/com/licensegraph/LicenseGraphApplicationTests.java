package com.licensegraph;

import com.licensegraph.policy.PolicyLookup;
import com.licensegraph.security.ClientContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies the E1 Spring Boot skeleton starts.
 *
 * <p>{@link PolicyLookup} (Module A) and {@link ClientContext} (Module E) have no implementation
 * yet, but Module B's asset service and controller need them. Until they land, they are replaced
 * by Mockito mocks here; {@code @MockitoBean} also replaces a real bean, so this keeps working
 * after they are merged, and the two fields can then be deleted.
 *
 * <p>AI-assisted (Claude): mock beans added with Claude by the Module B owner.
 */
@SpringBootTest
class LicenseGraphApplicationTests {

    @MockitoBean
    private PolicyLookup policyLookup;

    @MockitoBean
    private ClientContext clientContext;

    @Test
    void contextLoads() {
    }
}
