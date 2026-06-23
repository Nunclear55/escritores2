package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateChapterRequest(
        @NotNull
        Integer storyId,

        Integer volumeId,

        @NotBlank
        @Size(max = 255)
        String title,

        @Size(max = 255)
        String subtitle,

        String content,

        LocalDate publishedOn,

        @NotBlank
        String publicationState,

        @NotNull
        Integer positionIndex
) {
}