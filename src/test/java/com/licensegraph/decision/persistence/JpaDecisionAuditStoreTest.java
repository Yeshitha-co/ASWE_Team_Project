
package com.licensegraph.decision.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.licensegraph.common.AssetRef;
import com.licensegraph.common.Decision;
import com.licensegraph.common.IntendedUse;
import com.licensegraph.common.PolicyRef;
import com.licensegraph.decision.DecisionAuditStore;
import com.licensegraph.decision.UseDecisionRecord;
import com.licensegraph.decision.UseDecisionResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        JpaDecisionAuditStore.class,
        JpaDecisionAuditStoreTest.TestConfig.class
})

class JpaDecisionAuditStoreTest {

    @Autowired
    private DecisionAuditStore auditStore;

    @TestConfiguration
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Test
    void shouldSaveAndRetrieveDecision() {


        String clientId = "client-1";

        AssetRef target = new AssetRef("Model-C", 1);
        PolicyRef licenseC = new PolicyRef("License-C", 1);

        // Create a decision result
        UseDecisionResult result = new UseDecisionResult(
                target,
                IntendedUse.COMMERCIAL,
                Decision.ALLOW,
                List.of(),
                List.of(licenseC),
                List.of()
        );

        // Save it in H2
        UseDecisionRecord saved = auditStore.save(
                clientId,
                result
        );

        // Check that the database generated record has an ID
        assertNotNull(saved.decisionId());
        assertNotNull(saved.evaluatedAt());

        // Retrieve the saved decision
        Optional<UseDecisionRecord> retrieved =
                auditStore.findById(clientId, saved.decisionId());

        assertTrue(retrieved.isPresent());

        assertEquals(
                saved.decisionId(),
                retrieved.get().decisionId()
        );

        assertEquals(
                Decision.ALLOW,
                retrieved.get().result().decision()
        );

        assertEquals(
                target,
                retrieved.get().result().target()
        );

        // A different client must not see this decision
        assertTrue(
                auditStore.findById(
                        "another-client",
                        saved.decisionId()
                ).isEmpty()
        );

        // Find all decisions for this target
        List<UseDecisionRecord> history =
                auditStore.findByTarget(clientId, target);

        assertTrue(
                history.stream().anyMatch(
                        record -> record.decisionId()
                                .equals(saved.decisionId())
                )
        );
    }


}
