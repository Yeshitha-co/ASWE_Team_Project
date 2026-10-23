package com.licensegraph.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.licensegraph.common.AssetRef;
import com.licensegraph.common.exception.BadRequestException;
import com.licensegraph.common.exception.ConflictException;
import com.licensegraph.common.exception.ErrorCode;
import com.licensegraph.common.exception.NotFoundException;
import com.licensegraph.common.exception.UnauthorizedException;
import com.licensegraph.common.exception.UnprocessableEntityException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * API checks for client registration, the API-key filter, caller identity, and error mapping.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({
        ModuleEWebTest.CallerProbe.class,
        ModuleEWebTest.ProbeService.class,
        ModuleEWebTest.ErrorProbe.class
})
class ModuleEWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApiClientRepository clients;

    @Autowired
    private ClientContext clientContext;

    private String clientId;
    private String apiKey;

    @BeforeEach
    void registerClient() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/clients"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        clientId = body.get("clientId").asText();
        apiKey = body.get("apiKey").asText();
    }

    @Test
    void registrationReturnsKeyOnceAndStoresOnlyItsHash() {
        ApiClient stored = clients.findById(clientId).orElseThrow();

        assertThat(apiKey).isNotBlank();
        assertThat(stored.getKeyHash()).isEqualTo(ApiKeyHasher.sha256Hex(apiKey));
        assertThat(stored.getKeyHash()).isNotEqualTo(apiKey);
        assertThat(clients.findAll()).noneMatch(client -> apiKey.equals(client.getKeyHash()));
    }

    @Test
    void registrationIgnoresClientIdInTheBody() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"attacker\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String createdId = objectMapper.readTree(result.getResponse().getContentAsString()).get("clientId").asText();

        assertThat(createdId).isNotEqualTo("attacker");
    }

    @Test
    void validKeyReachesTheServiceAsTheRegisteredClient() throws Exception {
        mockMvc.perform(get("/api/test/caller").header(ApiKeyFilter.HEADER, apiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(clientId));
    }

    @Test
    void clientContextIsClearedAfterTheRequest() throws Exception {
        mockMvc.perform(get("/api/test/caller").header(ApiKeyFilter.HEADER, apiKey))
                .andExpect(status().isOk());

        assertThat(clientContext).isInstanceOf(RequestClientContext.class);
        assertThatThrownBy(clientContext::currentClientId)
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void missingKeyReturns401() throws Exception {
        mockMvc.perform(get("/api/test/caller"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("MISSING_API_KEY"))
                .andExpect(jsonPath("$.path").value("/api/test/caller"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void blankKeyIsMissing() throws Exception {
        mockMvc.perform(get("/api/test/caller").header(ApiKeyFilter.HEADER, " "))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("MISSING_API_KEY"));
    }

    @Test
    void invalidKeyReturns401() throws Exception {
        mockMvc.perform(get("/api/test/caller").header(ApiKeyFilter.HEADER, "not-a-real-key"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_API_KEY"))
                .andExpect(jsonPath("$.path").value("/api/test/caller"));
    }

    @Test
    void eachErrorCodeMapsToItsStatusAndBody() throws Exception {
        for (ErrorCode code : ErrorCode.values()) {
            mockMvc.perform(get("/api/test/errors/code/" + code.name()).header(ApiKeyFilter.HEADER, apiKey))
                    .andExpect(status().is(code.status()))
                    .andExpect(jsonPath("$.status").value(code.status()))
                    .andExpect(jsonPath("$.code").value(code.name()))
                    .andExpect(jsonPath("$.message").value("mapped " + code.name()))
                    .andExpect(jsonPath("$.path").value("/api/test/errors/code/" + code.name()))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Test
    void typedExceptionsKeepDetails() throws Exception {
        mockMvc.perform(get("/api/test/errors/typed/conflict").header(ApiKeyFilter.HEADER, apiKey))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CYCLE_DETECTED"))
                .andExpect(jsonPath("$.details.cyclePath[0]").value("dataset-A:v1"))
                .andExpect(jsonPath("$.details.cyclePath[3]").value("dataset-A:v1"));

        mockMvc.perform(get("/api/test/errors/typed/not-found").header(ApiKeyFilter.HEADER, apiKey))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ASSET_NOT_FOUND"));

        mockMvc.perform(get("/api/test/errors/typed/bad-request").header(ApiKeyFilter.HEADER, apiKey))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_POLICY_COMBINATION"));

        mockMvc.perform(get("/api/test/errors/typed/unprocessable").header(ApiKeyFilter.HEADER, apiKey))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("UNKNOWN_POLICY_REFERENCE"));
    }

    @Test
    void invalidBodyMapsToValidationError() throws Exception {
        mockMvc.perform(post("/api/test/errors/validation")
                        .header(ApiKeyFilter.HEADER, apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/test/errors/validation"));
    }

    @Test
    void unexpectedExceptionIsSafe500() throws Exception {
        mockMvc.perform(get("/api/test/errors/unexpected").header(ApiKeyFilter.HEADER, apiKey))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value(GlobalExceptionHandler.INTERNAL_ERROR))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.path").value("/api/test/errors/unexpected"))
                .andExpect(content -> assertThat(content.getResponse().getContentAsString())
                        .doesNotContain("db-password-should-not-leak")
                        .doesNotContain("IllegalStateException"));
    }

    /**
     * Stand-in for a module controller: reads {@link ClientContext} inside a service.
     */
    @RestController
    static class CallerProbe {

        private final ProbeService service;

        CallerProbe(ProbeService service) {
            this.service = service;
        }

        @GetMapping("/api/test/caller")
        Map<String, String> caller() {
            return Map.of("clientId", service.currentClientId());
        }
    }

    /**
     * Service used only by {@link CallerProbe}.
     */
    @org.springframework.stereotype.Service
    static class ProbeService {

        private final ClientContext clientContext;

        ProbeService(ClientContext clientContext) {
            this.clientContext = clientContext;
        }

        String currentClientId() {
            return clientContext.currentClientId();
        }
    }

    /**
     * Throws the shared exceptions so the advice can be checked without another module's controller.
     */
    @RestController
    static class ErrorProbe {

        @GetMapping("/api/test/errors/code/{code}")
        void byCode(@PathVariable String code) {
            ErrorCode errorCode = ErrorCode.valueOf(code);
            throw new com.licensegraph.common.exception.ApiException(errorCode, "mapped " + code);
        }

        @GetMapping("/api/test/errors/typed/{kind}")
        void typed(@PathVariable String kind) {
            switch (kind) {
                case "conflict" -> throw ConflictException.cycleDetected(List.of(
                        new AssetRef("dataset-A", 1),
                        new AssetRef("model-C", 1),
                        new AssetRef("model-B", 1),
                        new AssetRef("dataset-A", 1)));
                case "not-found" -> throw NotFoundException.asset("dataset-A:v1");
                case "bad-request" -> throw new BadRequestException(
                        ErrorCode.INVALID_POLICY_COMBINATION, "share-alike requires derivatives");
                case "unprocessable" -> throw UnprocessableEntityException.unknownPolicy("license-A:v9");
                case "unauthorized" -> throw UnauthorizedException.invalidApiKey();
                default -> throw new IllegalArgumentException(kind);
            }
        }

        @PostMapping("/api/test/errors/validation")
        void validation(@Valid @RequestBody NameBody body) {
            // Presence of a valid body is enough; the advice handles the invalid case.
        }

        @GetMapping("/api/test/errors/unexpected")
        void unexpected() {
            throw new IllegalStateException("db-password-should-not-leak");
        }
    }

    record NameBody(@NotBlank String name) {
    }
}
