package com.nunclear.escritores.enums;

import java.util.Arrays;

public enum AccountState {
    PENDING_VERIFICATION("pending_verification"),
    ACTIVE("active"),
    SUSPENDED("suspended"),
    BANNED("banned");

    private final String value;

    AccountState(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean isBlocked() {
        return this == SUSPENDED || this == BANNED;
    }

    public static AccountState fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("accountState inválido");
        }

        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(state -> state.value.equalsIgnoreCase(normalized) || state.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("accountState inválido"));
    }
}
