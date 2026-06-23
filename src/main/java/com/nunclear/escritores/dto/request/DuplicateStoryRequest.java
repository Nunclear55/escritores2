package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DuplicateStoryRequest(
        @NotBlank
        @Size(max = 255)
        String title
) {
}