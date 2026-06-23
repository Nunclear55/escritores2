package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LiftSanctionRequest(
        @NotBlank
        @Size(max = 5000)
        String reasonText
) {
}