package com.licensegraph.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.licensegraph.common.json.EnumJson;

/**
 * Duties attached to a policy (Section 10). JSON values: {@code attribution}, {@code share-alike}.
 */
public enum ObligationType {
    ATTRIBUTION,
    SHARE_ALIKE;

    @JsonValue
    public String toJson() {
        return EnumJson.toKebab(this);
    }

    @JsonCreator
    public static ObligationType fromJson(String value) {
        return EnumJson.fromKebab(ObligationType.class, value);
    }
}
