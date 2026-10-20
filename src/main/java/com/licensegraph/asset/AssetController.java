package com.licensegraph.asset;

import com.licensegraph.security.ClientContext;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * REST endpoints for assets (Module B, tasks B2 and B4).
 *
 * <ul>
 *   <li>{@code POST /api/assets} registers an asset version: 201 with the stored asset.</li>
 *   <li>{@code GET /api/assets/{assetId}/versions/{version}} reads one: 200, or 404 if missing
 *       or owned by another client.</li>
 * </ul>
 *
 * <p>The caller comes from {@link ClientContext}, never from the request body. Errors are thrown
 * as the shared exceptions and turned into the error format by Module E's handler.
 *
 * <p>AI-assisted (Claude): drafted with Claude, reviewed by the Module B owner.
 */
@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;
    private final ClientContext clientContext;

    AssetController(AssetService assetService, ClientContext clientContext) {
        this.assetService = assetService;
        this.clientContext = clientContext;
    }

    @PostMapping
    public ResponseEntity<AssetResponse> register(@RequestBody RegisterAssetRequest request) {
        AssetResponse created = assetService.register(clientContext.currentClientId(), request);
        URI location = UriComponentsBuilder.fromPath("/api/assets/{assetId}/versions/{version}")
                .buildAndExpand(created.assetId(), created.version())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{assetId}/versions/{version}")
    public AssetResponse get(@PathVariable("assetId") String assetId, @PathVariable("version") int version) {
        return assetService.get(clientContext.currentClientId(), assetId, version);
    }
}
