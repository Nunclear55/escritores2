package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(
        @NotBlank String verificationToken
) {
}