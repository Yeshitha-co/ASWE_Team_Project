package com.licensegraph.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.licensegraph.common.json.EnumJson;

/**
 * Asset types accepted at registration (Section 10).
 *
 * <p>JSON values: {@code dataset}, {@code model}, {@code code}, {@code derived-dataset}.
 */
public enum AssetType {
    DATASET,
    MODEL,
    CODE,
    DERIVED_DATASET;

    @JsonValue
    public String toJson() {
        return EnumJson.toKebab(this);
    }

    @JsonCreator
    public static AssetType fromJson(String value) {
        return EnumJson.fromKebab(AssetType.class, value);
    }
}
