
package com.licensegraph.decision;


import com.licensegraph.common.*;
import com.licensegraph.security.ClientContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UseDecisionControllerTest {

    private MockMvc mockMvc;
    private UseDecisionService useDecisionService;
    private DecisionAuditStore decisionAuditStore;
    private ClientContext clientContext;

    @BeforeEach
    void setUp() {

        useDecisionService = mock(UseDecisionService.class);
        decisionAuditStore = mock(DecisionAuditStore.class);
        clientContext = mock(ClientContext.class);

        UseDecisionController controller = new UseDecisionController(
                useDecisionService,
                decisionAuditStore,
                clientContext
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new UseDecisionExceptionHandler())
                .build();

        when(clientContext.currentClientId())
                .thenReturn("client-1");
    }

    @Test
    void shouldCreateDecisionUsingPost() throws Exception {

        AssetRef target = new AssetRef("Model-C", 1);

        UseDecisionResult result = new UseDecisionResult(
                target,
                IntendedUse.COMMERCIAL,
                Decision.ALLOW,
                List.of(),
                List.of(new PolicyRef("license-A", 1)),
                List.of()
        );

        UseDecisionRecord record = new UseDecisionRecord(
                "decision-123",
                Instant.parse("2026-10-20T12:00:00Z"),
                result
        );

        when(useDecisionService.decideAndRecord(
                "client-1",
                target,
                IntendedUse.COMMERCIAL
        )).thenReturn(record);

        mockMvc.perform(post("/api/use-decisions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assetId": "Model-C",
                                  "version": 1,
                                  "intendedUse": "commercial"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.decisionId")
                        .value("decision-123"))
                .andExpect(jsonPath("$.result.decision")
                        .value("ALLOW"))
                .andExpect(jsonPath("$.result.target")
                        .value("Model-C:v1"))
                .andExpect(jsonPath("$.result.evaluatedPolicies[0]")
                        .value("license-A:v1"));

        verify(useDecisionService).decideAndRecord(
                "client-1",
                target,
                IntendedUse.COMMERCIAL
        );
    }

    @Test
    void shouldRetrieveDecisionById() throws Exception {

        AssetRef target = new AssetRef("Model-C", 1);

        UseDecisionResult result = new UseDecisionResult(
                target,
                IntendedUse.COMMERCIAL,
                Decision.DENY,
                List.of(),
                List.of(),
                List.of()
        );

        UseDecisionRecord record = new UseDecisionRecord(
                "decision-456",
                Instant.parse("2026-10-20T12:00:00Z"),
                result
        );

        when(decisionAuditStore.findById(
                "client-1", "decision-456"
        )).thenReturn(Optional.of(record));

        mockMvc.perform(get("/api/use-decisions/decision-456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisionId")
                        .value("decision-456"))
                .andExpect(jsonPath("$.result.decision")
                        .value("DENY"))
                .andExpect(jsonPath("$.result.target")
                        .value("Model-C:v1"));

        verify(decisionAuditStore).findById(
                "client-1", "decision-456"
        );
    }

    @Test
    void shouldRetrieveDecisionHistory() throws Exception {

        AssetRef target = new AssetRef("Model-C", 1);

        UseDecisionResult result = new UseDecisionResult(
                target,
                IntendedUse.COMMERCIAL,
                Decision.ALLOW,
                List.of(),
                List.of(),
                List.of()
        );

        UseDecisionRecord record = new UseDecisionRecord(
                "decision-789",
                Instant.parse("2026-10-20T12:00:00Z"),
                result
        );

        when(decisionAuditStore.findByTarget(
                "client-1", target
        )).thenReturn(List.of(record));

        mockMvc.perform(get("/api/use-decisions")
                        .param("assetId", "Model-C")
                        .param("version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].decisionId")
                        .value("decision-789"))
                .andExpect(jsonPath("$[0].result.decision")
                        .value("ALLOW"))
                .andExpect(jsonPath("$[0].result.target")
                        .value("Model-C:v1"));

        verify(decisionAuditStore).findByTarget(
                "client-1", target
        );
    }

    @Test
    void shouldReturn404WhenDecisionDoesNotExist() throws Exception {

        when(decisionAuditStore.findById(
                "client-1", "missing-decision"
        )).thenReturn(Optional.empty());

        mockMvc.perform(
                        get("/api/use-decisions/missing-decision")
                )
                .andExpect(status().isNotFound());

        verify(decisionAuditStore).findById(
                "client-1", "missing-decision"
        );
    }

    @Test
    void shouldRejectInvalidPostRequest() throws Exception {

        mockMvc.perform(post("/api/use-decisions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "assetId": "",
                              "version": 0,
                              "intendedUse": null
                            }
                            """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(useDecisionService);
    }


    @Test
    void shouldNotExposeAnotherClientsDecision() throws Exception {

        // Simulate a request authenticated as client-2.
        when(clientContext.currentClientId())
                .thenReturn("client-2");

        // Client-2 cannot find client-1's decision.
        when(decisionAuditStore.findById(
                "client-2", "decision-123"
        )).thenReturn(Optional.empty());

        mockMvc.perform(
                        get("/api/use-decisions/decision-123")
                )
                .andExpect(status().isNotFound());

        // Verify the lookup used the authenticated client ID.
        verify(decisionAuditStore).findById(
                "client-2", "decision-123"
        );

        // Make sure the controller didn't look up client-1's data.
        verify(decisionAuditStore, never()).findById(
                eq("client-1"), anyString()
        );
    }



}
