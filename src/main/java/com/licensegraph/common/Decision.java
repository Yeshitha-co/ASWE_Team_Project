package com.licensegraph.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.licensegraph.common.json.EnumJson;

/**
 * Use-decision outcome (Section 10). JSON values: {@code allow}, {@code deny}.
 */
public enum Decision {
    ALLOW,
    DENY;

    @JsonValue
    public String toJson() {
        return EnumJson.toKebab(this);
    }

    @JsonCreator
    public static Decision fromJson(String value) {
        return EnumJson.fromKebab(Decision.class, value);
    }
}
