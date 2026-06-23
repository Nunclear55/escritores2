package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateItemRequest(
        @NotNull Integer storyId,

        @NotBlank
        @Size(max = 255)
        String name,

        @Size(max = 5000)
        String description,

        Integer quantity,

        @Size(max = 50)
        String unitName
) {
}