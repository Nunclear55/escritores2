package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateVolumeRequest(
        @NotNull Integer storyId,
        Integer arcId,

        @NotBlank
        @Size(max = 255)
        String title,

        @NotNull Integer positionIndex
) {
}