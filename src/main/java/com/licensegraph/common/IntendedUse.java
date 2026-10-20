package com.licensegraph.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.licensegraph.common.json.EnumJson;

/**
 * Intended uses accepted by use-decision requests (Section 10).
 *
 * <p>JSON values: {@code commercial}, {@code redistribution}, {@code derivative},
 * {@code ai-training}, {@code internal}.
 */
public enum IntendedUse {
    COMMERCIAL,
    REDISTRIBUTION,
    DERIVATIVE,
    AI_TRAINING,
    INTERNAL;

    @JsonValue
    public String toJson() {
        return EnumJson.toKebab(this);
    }

    @JsonCreator
    public static IntendedUse fromJson(String value) {
        return EnumJson.fromKebab(IntendedUse.class, value);
    }
}
