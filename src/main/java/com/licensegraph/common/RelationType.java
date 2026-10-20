package com.licensegraph.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.licensegraph.common.json.EnumJson;

/**
 * Derivation relations stored input-to-output (Section 10).
 *
 * <p>JSON values: {@code trained-on}, {@code fine-tuned-from}, {@code merged-from},
 * {@code filtered-from}, {@code derived-from}.
 */
public enum RelationType {
    TRAINED_ON,
    FINE_TUNED_FROM,
    MERGED_FROM,
    FILTERED_FROM,
    DERIVED_FROM;

    @JsonValue
    public String toJson() {
        return EnumJson.toKebab(this);
    }

    @JsonCreator
    public static RelationType fromJson(String value) {
        return EnumJson.fromKebab(RelationType.class, value);
    }
}
