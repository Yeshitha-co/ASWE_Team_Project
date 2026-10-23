package com.licensegraph.lineage;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.DerivationInput;
import com.licensegraph.common.RelationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * JSON body for POST /api/derivations: one output and one or more inputs (C2).
 * Bean Validation rejects missing or malformed fields with 400 before the service runs.
 */
public record DerivationRequest(
        @NotNull @Valid AssetBody output,
        @NotEmpty List<@NotNull @Valid InputBody> inputs) {

    /**
     * An asset reference as it appears in the request body.
     */
    public record AssetBody(@NotBlank String assetId, @NotNull @Positive Integer version) {
        public AssetRef toRef() {
            return new AssetRef(assetId, version);
        }
    }

    /**
     * One input asset plus how the output was derived from it.
     */
    public record InputBody(@NotBlank String assetId,
                            @NotNull @Positive Integer version,
                            @NotNull RelationType relation) {
        public DerivationInput toInput() {
            return new DerivationInput(new AssetRef(assetId, version), relation);
        }
    }

    /**
     * Converts the request inputs into the shared records the service expects.
     */
    public List<DerivationInput> toInputs() {
        return inputs.stream().map(InputBody::toInput).toList();
    }
}