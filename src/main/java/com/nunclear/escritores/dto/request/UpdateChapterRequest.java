package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateChapterRequest(
        @NotBlank
        @Size(max = 255)
        String title,

        @Size(max = 255)
        String subtitle,

        String content,

        Integer volumeId,

        Integer positionIndex
) {
}