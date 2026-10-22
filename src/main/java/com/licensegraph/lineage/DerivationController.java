package com.licensegraph.lineage;

import com.licensegraph.common.AssetRef;
import com.licensegraph.lineage.LineageResponses.DerivationResponse;
import com.licensegraph.lineage.LineageResponses.GraphResponse;
import com.licensegraph.security.ClientContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for Module C: record a derivation, and read ancestors or descendants (C2, C7).
 * The caller's client id always comes from {@link ClientContext}, never from the request body.
 */
// AI-assisted: drafted with Claude, reviewed by Srishti Singh.
@RestController
@RequestMapping("/api")
public class DerivationController {

    private final DerivationService derivations;
    private final LineageQueryService lineage;
    private final ClientContext clientContext;

    public DerivationController(DerivationService derivations,
                                LineageQueryService lineage,
                                ClientContext clientContext) {
        this.derivations = derivations;
        this.lineage = lineage;
        this.clientContext = clientContext;
    }

    /** POST /api/derivations: 201 with created edges, conflicts and the output's ancestor count. */
    @PostMapping("/derivations")
    @ResponseStatus(HttpStatus.CREATED)
    public DerivationResponse recordDerivation(@Valid @RequestBody DerivationRequest request) {
        AssetRef output = request.output().toRef();
        DerivationResult result = derivations.recordDerivation(
                clientContext.currentClientId(), output, request.toInputs());
        return DerivationResponse.from(output, result);
    }

    /** GET /api/assets/{assetId}/versions/{version}/ancestors: 200 with the upstream subgraph. */
    @GetMapping("/assets/{assetId}/versions/{version}/ancestors")
    public GraphResponse ancestors(@PathVariable String assetId, @PathVariable int version) {
        return GraphResponse.from(
                lineage.findAncestors(clientContext.currentClientId(), new AssetRef(assetId, version)));
    }

    /** GET /api/assets/{assetId}/versions/{version}/descendants: 200 with the downstream subgraph. */
    @GetMapping("/assets/{assetId}/versions/{version}/descendants")
    public GraphResponse descendants(@PathVariable String assetId, @PathVariable int version) {
        return GraphResponse.from(
                lineage.findDescendants(clientContext.currentClientId(), new AssetRef(assetId, version)));
    }
}