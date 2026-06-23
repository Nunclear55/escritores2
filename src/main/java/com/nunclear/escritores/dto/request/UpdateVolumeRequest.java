package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateVolumeRequest(
        @NotBlank
        @Size(max = 255)
        String title,

        Integer arcId,

        @NotNull Integer positionIndex
) {
}