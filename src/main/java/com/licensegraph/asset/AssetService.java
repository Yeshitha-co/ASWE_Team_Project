package com.licensegraph.asset;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.AssetType;
import com.licensegraph.common.PolicyRef;
import com.licensegraph.common.exception.BadRequestException;
import com.licensegraph.common.exception.ConflictException;
import com.licensegraph.common.exception.ErrorCode;
import com.licensegraph.common.exception.NotFoundException;
import com.licensegraph.common.exception.UnprocessableEntityException;
import com.licensegraph.policy.PolicyLookup;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI-assisted: Registers and reads asset versions (Module B, tasks B2 to B4).
 *
 * <p>Every method takes the caller's {@code clientId} explicitly, so tests need no security
 * setup and no request can act for another client. Assets are insert-only: there is no update
 * or delete, and linking an asset to a different policy means registering a new version.
 */
@Service
public class AssetService {

    /**
     * Ids appear in URL paths and in printed names like {@code dataset-A:v1}, so '/', ':' and
     * spaces are not allowed.
     */
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9._-]{1,255}");

    static final int MAX_METADATA_LENGTH = 2000;

    private final AssetRepository repository;
    private final PolicyLookup policyLookup;

    AssetService(AssetRepository repository, PolicyLookup policyLookup) {
        this.repository = repository;
        this.policyLookup = policyLookup;
    }

    /**
     * Registers a new asset version for the caller.
     *
     * <p>Checks run in this order: request fields (400 {@code VALIDATION_ERROR}), asset type
     * (400 {@code INVALID_ASSET_TYPE}), duplicate version (409 {@code DUPLICATE_ASSET_VERSION}),
     * then the policy reference (422 {@code UNKNOWN_POLICY_REFERENCE}).
     *
     * @param clientId the caller, from {@code ClientContext}
     * @param request the request body
     * @return the stored asset
     */
    @Transactional
    public AssetResponse register(String clientId, RegisterAssetRequest request) {
        if (request == null) {
            throw validationError("Request body is required", "body");
        }
        String assetId = requireId("assetId", request.assetId());
        int version = requirePositive("version", request.version());
        if (request.type() == null || request.type().isBlank()) {
            throw validationError("type is required", "type");
        }
        String policyId = requireId("policyId", request.policyId());
        int policyVersion = requirePositive("policyVersion", request.policyVersion());
        if (request.metadata() != null && request.metadata().length() > MAX_METADATA_LENGTH) {
            throw validationError(
                    "metadata must be at most " + MAX_METADATA_LENGTH + " characters", "metadata");
        }
        AssetType type = parseType(request.type());

        AssetRef ref = new AssetRef(assetId, version);
        PolicyRef policy = new PolicyRef(policyId, policyVersion);

        if (repository.existsByClientIdAndAssetIdAndVersion(clientId, assetId, version)) {
            throw ConflictException.duplicateAssetVersion(ref.toString());
        }
        if (policyLookup.findPolicy(clientId, policy).isEmpty()) {
            throw UnprocessableEntityException.unknownPolicy(policy.toString());
        }

        try {
            Asset saved = repository.saveAndFlush(
                    new Asset(clientId, ref, type, policy, request.metadata()));
            return AssetResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            // Two identical requests at the same moment: both passed the exists check, and the
            // unique constraint on (client_id, asset_id, version) rejected the second insert.
            throw ConflictException.duplicateAssetVersion(ref.toString());
        }
    }

    /**
     * Reads one asset version owned by the caller.
     *
     * @throws NotFoundException {@code ASSET_NOT_FOUND} if it does not exist or belongs to another
     *     client (never 403, so other clients' assets stay invisible)
     */
    @Transactional(readOnly = true)
    public AssetResponse get(String clientId, String assetId, int version) {
        String name = assetId + ":v" + version;
        if (version < 1) {
            throw NotFoundException.asset(name);
        }
        return repository.findByClientIdAndAssetIdAndVersion(clientId, assetId, version)
                .map(AssetResponse::from)
                .orElseThrow(() -> NotFoundException.asset(name));
    }

    private static String requireId(String field, String value) {
        if (value == null || value.isBlank()) {
            throw validationError(field + " is required", field);
        }
        if (!ID_PATTERN.matcher(value).matches()) {
            throw validationError(
                    field + " must be 1-255 characters of letters, digits, '.', '_' or '-'", field);
        }
        return value;
    }

    private static int requirePositive(String field, Integer value) {
        if (value == null) {
            throw validationError(field + " is required", field);
        }
        if (value < 1) {
            throw validationError(field + " must be a positive integer", field);
        }
        return value;
    }

    private static AssetType parseType(String value) {
        try {
            return AssetType.fromJson(value);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ErrorCode.INVALID_ASSET_TYPE,
                    "Unknown asset type: " + value
                            + " (expected dataset, model, code or derived-dataset)",
                    Map.of("field", "type"));
        }
    }

    private static BadRequestException validationError(String message, String field) {
        return new BadRequestException(ErrorCode.VALIDATION_ERROR, message, Map.of("field", field));
    }
}
