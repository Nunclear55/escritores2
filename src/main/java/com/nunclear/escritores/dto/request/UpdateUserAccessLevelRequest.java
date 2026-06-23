package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserAccessLevelRequest(
        @NotBlank
        String accessLevel
) {
}