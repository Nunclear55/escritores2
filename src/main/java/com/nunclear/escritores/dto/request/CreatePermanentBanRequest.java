package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePermanentBanRequest(
        @NotNull Integer targetUserId,

        @NotBlank
        @Size(max = 5000)
        String reasonText
) {
}