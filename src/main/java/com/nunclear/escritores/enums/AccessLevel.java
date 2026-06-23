package com.nunclear.escritores.enums;

import java.util.Arrays;

public enum AccessLevel {
    USER("user"),
    MODERATOR("moderator"),
    ADMIN("admin");

    private final String value;

    AccessLevel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean isModeratorOrAdmin() {
        return this == MODERATOR || this == ADMIN;
    }

    public static AccessLevel fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("accessLevel inválido");
        }

        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(level -> level.value.equalsIgnoreCase(normalized) || level.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("accessLevel inválido"));
    }
}
