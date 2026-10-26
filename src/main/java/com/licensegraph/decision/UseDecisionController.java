package com.licensegraph.decision;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.IntendedUse;
import com.licensegraph.common.exception.ApiException;
import com.licensegraph.common.exception.ErrorCode;
import com.licensegraph.common.exception.NotFoundException;
import com.licensegraph.security.ClientContext;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing Module D's use decision and audit history APIs.
 *
 * <p>Provides endpoints for evaluating and recording an intended use,
 * retrieving a previously saved decision, and listing decision history
 * for a specific asset version.
 *
 * <p>All operations are scoped to the authenticated client. The client
 * identifier is obtained from {@link ClientContext}, which is populated
 * by Module E's authentication mechanism. Client identifiers supplied
 * by HTTP callers are not trusted.
 */
@RestController
@RequestMapping("/api/use-decisions")
public class UseDecisionController {

    private final UseDecisionService useDecisionService;
    private final DecisionAuditStore decisionAuditStore;
    private final ClientContext clientContext;

    /**
     * Creates the controller with its service and security dependencies.
     *
     * @param useDecisionService evaluates and records requested uses
     * @param decisionAuditStore retrieves previously recorded decisions
     * @param clientContext provides the authenticated client identifier
     */
    public UseDecisionController(
            UseDecisionService useDecisionService,
            DecisionAuditStore decisionAuditStore,
            ClientContext clientContext) {

        this.useDecisionService = useDecisionService;
        this.decisionAuditStore = decisionAuditStore;
        this.clientContext = clientContext;
    }

    /**
     * Evaluates an intended asset use and saves the resulting decision.
     *
     * <p>Endpoint: POST /api/use-decisions
     *
     * <p>The request body identifies the asset version and intended use.
     * The authenticated client identifier is resolved from ClientContext.
     *
     * <p>This operation creates a new audit record containing the
     * evaluation result, generated decision ID, and timestamp.
     *
     * @param request validated asset version and intended use
     * @return newly recorded use decision
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UseDecisionResponse createDecision(
            @Valid @RequestBody CreateUseDecisionRequest request) {

        String clientId = clientContext.currentClientId();

        AssetRef target = new AssetRef(
                request.assetId(),
                request.version());

        IntendedUse intendedUse;

        try {
            intendedUse = IntendedUse.fromJson(request.intendedUse());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    ErrorCode.INVALID_INTENDED_USE,
                    "Invalid intended use: " + request.intendedUse());
        }

        UseDecisionRecord record = useDecisionService.decideAndRecord(
                clientId,
                target,
                intendedUse);

        return UseDecisionResponse.from(record);
    }

    /**
     * Retrieves a previously saved decision by its identifier.
     *
     * <p>Endpoint: GET /api/use-decisions/{decisionId}
     *
     * <p>The lookup is restricted to the authenticated client.
     * A missing decision, including one belonging to another client,
     * results in a not-found exception.
     *
     * <p>This operation reads the existing audit record without
     * re-evaluating the policies or modifying stored data.
     *
     * @param decisionId unique identifier of the recorded decision
     * @return matching decision audit record
     * @throws NotFoundException if no matching client-scoped decision exists
     */
    @GetMapping("/{decisionId}")
    public UseDecisionResponse getDecision(
            @PathVariable String decisionId) {

        String clientId = clientContext.currentClientId();

        UseDecisionRecord record = decisionAuditStore.findById(clientId, decisionId)
                .orElseThrow(() -> NotFoundException.decision(decisionId));

        return UseDecisionResponse.from(record);
    }

    /**
     * Retrieves saved decision history for an asset version.
     *
     * <p>Endpoint: GET /api/use-decisions?assetId=...&version=...
     *
     * <p>Returns audit records matching the authenticated client,
     * asset identifier, and version. This operation does not perform
     * a new evaluation or create an audit record.
     *
     * @param assetId identifier of the evaluated asset
     * @param version version of the evaluated asset
     * @return saved decisions matching the requested asset version
     */
    @GetMapping
    public List<UseDecisionResponse> getDecisionHistory(
            @RequestParam(required = false) String assetId,
            @RequestParam(required = false) Integer version) {

        if (assetId == null || assetId.isBlank()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "assetId must not be blank");
        }

        if (version == null || version < 1) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "version must be a positive integer");
        }

        String clientId = clientContext.currentClientId();
        AssetRef target = new AssetRef(assetId, version);

        return decisionAuditStore.findByTarget(clientId, target)
                .stream()
                .map(UseDecisionResponse::from)
                .toList();
    }
}