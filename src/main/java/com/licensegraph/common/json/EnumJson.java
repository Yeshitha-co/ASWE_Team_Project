package com.licensegraph.common.json;

import java.util.Locale;

/**
 * Converts enums to and from lowercase kebab-case JSON values (Section 10).
 */
public final class EnumJson {

    private EnumJson() {
    }

    public static String toKebab(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public static <E extends Enum<E>> E fromKebab(Class<E> type, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(type.getSimpleName() + " is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return Enum.valueOf(type, normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown " + type.getSimpleName() + " value: " + value, ex);
        }
    }
}
