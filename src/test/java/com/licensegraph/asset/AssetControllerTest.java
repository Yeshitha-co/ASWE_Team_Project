package com.licensegraph.asset;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.licensegraph.common.AssetType;
import com.licensegraph.security.ClientContext;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link AssetController}: routes, status codes, JSON shape, and that the
 * client always comes from {@link ClientContext} (Module B, tasks B2 and B4).
 *
 * <p>Error responses are produced by Module E's exception handler and are covered by the API
 * tests once it exists; {@link AssetServiceTest} checks which exception each error raises.
 *
 * <p>The API-key filter is switched off here ({@code addFilters = false}) because
 * {@link ClientContext} is mocked; authentication is tested by Module E.
 *
 * <p>AI-assisted (Claude): tests drafted with Claude, reviewed by the Module B owner.
 */
@WebMvcTest(AssetController.class)
@AutoConfigureMockMvc(addFilters = false)
class AssetControllerTest {

    private static final AssetResponse DATASET_A_V1 = new AssetResponse("dataset-A", 1, "dataset-A:v1",
            AssetType.DATASET, "license-A", 1, "license-A:v1", null, Instant.parse("2026-10-21T14:03:11Z"));

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AssetService assetService;

    @MockitoBean
    private ClientContext clientContext;

    @BeforeEach
    void callerIsClientOne() {
        when(clientContext.currentClientId()).thenReturn("client-1");
    }

    @Test
    void postRegistersAssetAndReturns201WithLocation() throws Exception {
        RegisterAssetRequest request = new RegisterAssetRequest("dataset-A", 1, "dataset", "license-A", 1, null);
        when(assetService.register("client-1", request)).thenReturn(DATASET_A_V1);

        mvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "assetId": "dataset-A", "version": 1, "type": "dataset",
                                  "policyId": "license-A", "policyVersion": 1 }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/assets/dataset-A/versions/1"))
                .andExpect(jsonPath("$.name").value("dataset-A:v1"))
                .andExpect(jsonPath("$.type").value("dataset"))
                .andExpect(jsonPath("$.policy").value("license-A:v1"))
                .andExpect(jsonPath("$.policyVersion").value(1))
                .andExpect(jsonPath("$.createdAt").value("2026-10-21T14:03:11Z"))
                .andExpect(jsonPath("$.metadata").doesNotExist());
    }

    @Test
    void postIgnoresClientIdInBody() throws Exception {
        RegisterAssetRequest request = new RegisterAssetRequest("dataset-A", 1, "dataset", "license-A", 1, null);
        when(assetService.register("client-1", request)).thenReturn(DATASET_A_V1);

        mvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "clientId": "client-2", "assetId": "dataset-A", "version": 1,
                                  "type": "dataset", "policyId": "license-A", "policyVersion": 1 }
                                """))
                .andExpect(status().isCreated());

        verify(assetService).register(eq("client-1"), eq(request));
    }

    @Test
    void getReturnsAssetWithKebabCaseType() throws Exception {
        AssetResponse derived = new AssetResponse("subset-E", 2, "subset-E:v2", AssetType.DERIVED_DATASET,
                "license-A", 1, "license-A:v1", "filtered", Instant.parse("2026-10-21T14:03:11Z"));
        when(assetService.get("client-1", "subset-E", 2)).thenReturn(derived);

        mvc.perform(get("/api/assets/subset-E/versions/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("subset-E:v2"))
                .andExpect(jsonPath("$.type").value("derived-dataset"))
                .andExpect(jsonPath("$.metadata").value("filtered"));
    }
}
