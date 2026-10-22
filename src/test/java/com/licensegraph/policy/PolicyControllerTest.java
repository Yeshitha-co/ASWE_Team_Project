
package com.licensegraph.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.licensegraph.security.ClientContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PolicyController.class)
class PolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PolicyService policyService;

    @MockitoBean
    private ClientContext clientContext;

    @Test
    void createPolicyWithValidRequestReturnsCreated() throws Exception {
        when(clientContext.currentClientId()).thenReturn("client-1");

        LicensePolicy savedPolicy = new LicensePolicy(
                "client-1",
                "cc-by",
                1,
                true,
                true,
                true,
                false,
                true,
                false);

        when(policyService.createPolicy(
                org.mockito.ArgumentMatchers.eq("client-1"),
                any(LicensePolicy.class)))
                .thenReturn(savedPolicy);

        CreatePolicyRequest request = new CreatePolicyRequest(
                "cc-by",
                1,
                true,
                true,
                true,
                false,
                true,
                false);

        mockMvc.perform(post("/api/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.policyId").value("cc-by"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.aiTraining").value(false));

        verify(clientContext).currentClientId();
        verify(policyService).createPolicy(
                org.mockito.ArgumentMatchers.eq("client-1"),
                any(LicensePolicy.class));
    }


    @Test
    void createPolicyWithMissingRequiredFieldReturnsBadRequest()
            throws Exception {

        String requestJson = """
            {
                "policyId": "cc-by",
                "version": 1,
                "commercialUse": true,
                "redistribution": true,
                "derivatives": true,
                "aiTraining": false,
                "attributionRequired": true
            }
            """;

        mockMvc.perform(post("/api/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    /**
    @Test
    void createPolicyWithInvalidCombinationReturnsBadRequest()
            throws Exception {

        when(clientContext.currentClientId()).thenReturn("client-1");

        String requestJson = """
            {
                "policyId": "cc-by",
                "version": 1,
                "commercialUse": true,
                "redistribution": true,
                "derivatives": false,
                "aiTraining": false,
                "attributionRequired": true,
                "shareAlike": true
            }
            """;

        when(policyService.createPolicy(
                org.mockito.ArgumentMatchers.eq("client-1"),
                any(LicensePolicy.class)))
                .thenThrow(new com.licensegraph.common.exception.BadRequestException(
                        com.licensegraph.common.exception.ErrorCode.INVALID_POLICY_COMBINATION,
                        "Share-alike requires derivatives to be allowed"));

        mockMvc.perform(post("/api/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        verify(policyService).createPolicy(
                org.mockito.ArgumentMatchers.eq("client-1"),
                any(LicensePolicy.class));
    }
    */
}
