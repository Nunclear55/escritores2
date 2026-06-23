package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateStoryRequest(
        @NotBlank
        @Size(max = 255)
        String title,

        @Size(max = 5000)
        String description,

        @Size(max = 500)
        String coverImageUrl,

        @NotBlank
        String visibilityState,

        @NotBlank
        String publicationState,

        @NotNull
        Boolean allowFeedback,

        @NotNull
        Boolean allowScores,

        LocalDate startedOn
) {
}