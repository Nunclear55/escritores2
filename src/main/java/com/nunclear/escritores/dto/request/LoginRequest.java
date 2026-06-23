package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String loginOrEmail,
        @NotBlank String password
) {
}