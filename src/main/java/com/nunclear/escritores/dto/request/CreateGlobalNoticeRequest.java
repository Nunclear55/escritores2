package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateGlobalNoticeRequest(
        @NotBlank
        @Size(max = 255)
        String title,

        @NotBlank
        @Size(max = 5000)
        String messageText,

        @NotNull
        Boolean isEnabled,

        LocalDateTime startsAt,
        LocalDateTime endsAt
) {
}