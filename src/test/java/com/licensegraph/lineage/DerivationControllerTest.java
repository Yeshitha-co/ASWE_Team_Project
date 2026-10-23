package com.licensegraph.lineage;

import static com.licensegraph.lineage.LineageFixtures.CLIENT;
import static com.licensegraph.lineage.LineageFixtures.DATASET_A;
import static com.licensegraph.lineage.LineageFixtures.MODEL_B;
import static com.licensegraph.lineage.LineageFixtures.MODEL_C;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.licensegraph.asset.PolicyConflict;
import com.licensegraph.common.LineageEdge;
import com.licensegraph.common.LineageGraph;
import com.licensegraph.common.PolicyRef;
import com.licensegraph.common.RelationType;
import com.licensegraph.security.ClientContext;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests the Module C REST layer with the services mocked: status codes, request validation,
 * response shape, and that the client id comes from ClientContext (C2, C7, C9).
 */
// AI-assisted: drafted with Claude, reviewed by Srishti Singh.
@WebMvcTest(DerivationController.class)
@AutoConfigureMockMvc(addFilters = false)
class DerivationControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DerivationService derivations;

    @MockitoBean
    private LineageQueryService lineage;

    @MockitoBean
    private ClientContext clientContext;

    @BeforeEach
    void callerIsClientA() {
        when(clientContext.currentClientId()).thenReturn(CLIENT);
    }

    @Test
    void postReturns201WithCreatedEdgesAndAncestorCount() throws Exception {
        when(derivations.recordDerivation(eq(CLIENT), eq(MODEL_C), any())).thenReturn(new DerivationResult(
                List.of(new LineageEdge(MODEL_B, MODEL_C, RelationType.FINE_TUNED_FROM)),
                List.of(new PolicyConflict(DATASET_A, new PolicyRef("license-A", 1), "derivatives")), 2));

        mvc.perform(post("/api/derivations").contentType(MediaType.APPLICATION_JSON).content("""
                        {"output": {"assetId": "model-C", "version": 1},
                         "inputs": [{"assetId": "model-B", "version": 1, "relation": "fine-tuned-from"}]}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.output").value("model-C:v1"))
                .andExpect(jsonPath("$.created[0].input").value("model-B:v1"))
                .andExpect(jsonPath("$.created[0].output").value("model-C:v1"))
                .andExpect(jsonPath("$.ancestorCount").value(2))
                .andExpect(jsonPath("$.conflicts[0].source").value("dataset-A:v1"))
                .andExpect(jsonPath("$.conflicts[0].policy").value("license-A:v1"))
                .andExpect(jsonPath("$.conflicts[0].field").value("derivatives"));
    }

    @Test
    void postWithoutInputsIs400AndNeverReachesTheService() throws Exception {
        mvc.perform(post("/api/derivations").contentType(MediaType.APPLICATION_JSON).content("""
                        {"output": {"assetId": "model-C", "version": 1}, "inputs": []}
                        """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(derivations);
    }

    @Test
    void postWithMissingVersionIs400() throws Exception {
        mvc.perform(post("/api/derivations").contentType(MediaType.APPLICATION_JSON).content("""
                        {"output": {"assetId": "model-C"},
                         "inputs": [{"assetId": "model-B", "version": 1, "relation": "fine-tuned-from"}]}
                        """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(derivations);
    }

    @Test
    void ancestorsReturns200WithSortedNodes() throws Exception {
        when(lineage.findAncestors(CLIENT, MODEL_C)).thenReturn(LineageFixtures.chainAncestorsOfModelC());

        mvc.perform(get("/api/assets/model-C/versions/1/ancestors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.root").value("model-C:v1"))
                .andExpect(jsonPath("$.nodes.length()").value(3))
                .andExpect(jsonPath("$.nodes[0]").value("dataset-A:v1"))
                .andExpect(jsonPath("$.edges.length()").value(2));
    }

    @Test
    void descendantsReturns200() throws Exception {
        when(lineage.findDescendants(CLIENT, DATASET_A)).thenReturn(new LineageGraph(
                DATASET_A, Set.of(DATASET_A, MODEL_B, MODEL_C), LineageFixtures.chainEdges()));

        mvc.perform(get("/api/assets/dataset-A/versions/1/descendants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.root").value("dataset-A:v1"))
                .andExpect(jsonPath("$.nodes.length()").value(3));
    }
}