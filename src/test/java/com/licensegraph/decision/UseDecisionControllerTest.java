package com.licensegraph.decision;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.Decision;
import com.licensegraph.common.IntendedUse;
import com.licensegraph.common.PolicyRef;
import com.licensegraph.security.ClientContext;
import com.licensegraph.security.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;


/**
 * Tests the Module D use-decision REST controller.
 */
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
                .setControllerAdvice(new GlobalExceptionHandler())
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

        mockMvc.perform(get("/api/use-decisions/missing-decision"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("DECISION_NOT_FOUND"));

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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));

        verifyNoInteractions(useDecisionService);
    }

    @Test
    void shouldRejectUnknownIntendedUse() throws Exception {

        mockMvc.perform(post("/api/use-decisions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assetId": "Model-C",
                                  "version": 1,
                                  "intendedUse": "unknown-use"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INTENDED_USE"));

        verifyNoInteractions(useDecisionService);
    }

    @Test
    void shouldRejectMissingAssetIdInHistory() throws Exception {

        mockMvc.perform(get("/api/use-decisions")
                        .param("version", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));

        verifyNoInteractions(decisionAuditStore);
    }

    @Test
    void shouldRejectBlankAssetIdInHistory() throws Exception {

        mockMvc.perform(get("/api/use-decisions")
                        .param("assetId", "")
                        .param("version", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));

        verifyNoInteractions(decisionAuditStore);
    }

    @Test
    void shouldRejectMissingVersionInHistory() throws Exception {

        mockMvc.perform(get("/api/use-decisions")
                        .param("assetId", "Model-C"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));

        verifyNoInteractions(decisionAuditStore);
    }

    @Test
    void shouldRejectZeroVersionInHistory() throws Exception {

        mockMvc.perform(get("/api/use-decisions")
                        .param("assetId", "Model-C")
                        .param("version", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));

        verifyNoInteractions(decisionAuditStore);
    }

    @Test
    void shouldRejectNegativeVersionInHistory() throws Exception {

        mockMvc.perform(get("/api/use-decisions")
                        .param("assetId", "Model-C")
                        .param("version", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));

        verifyNoInteractions(decisionAuditStore);
    }

    @Test
    void shouldRejectNonnumericVersionInHistory() throws Exception {

        mockMvc.perform(get("/api/use-decisions")
                        .param("assetId", "Model-C")
                        .param("version", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));

        verifyNoInteractions(decisionAuditStore);
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

        mockMvc.perform(get("/api/use-decisions/decision-123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("DECISION_NOT_FOUND"));

        // Verify the lookup used the authenticated client ID.
        verify(decisionAuditStore).findById(
                "client-2", "decision-123"
        );

        // Ensure client-1's data was not accessed.
        verify(decisionAuditStore, never()).findById(
                eq("client-1"), anyString()
        );
    }
}