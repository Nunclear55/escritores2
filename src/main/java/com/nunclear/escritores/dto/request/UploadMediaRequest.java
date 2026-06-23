package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UploadMediaRequest(
        @NotBlank
        @Size(max = 255)
        String originalFilename,

        @NotBlank
        @Size(max = 50)
        String mediaKind,

        @Size(max = 255)
        String description,

        @NotNull
        Integer chapterId,

        @NotBlank
        @Size(max = 500)
        String storagePath
) {
}