package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateIdeaRequest(
        @NotBlank
        @Size(max = 255)
        String title,

        @Size(max = 5000)
        String content
) {
}