package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateTemporaryBanRequest(
        @NotNull Integer targetUserId,

        @NotBlank
        @Size(max = 5000)
        String reasonText,

        @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime endsAt
) {
}