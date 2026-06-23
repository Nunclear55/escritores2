package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReplaceMediaRequest(
        @NotBlank
        @Size(max = 255)
        String originalFilename,

        @Size(max = 255)
        String description,

        @NotBlank
        @Size(max = 500)
        String storagePath
) {
}