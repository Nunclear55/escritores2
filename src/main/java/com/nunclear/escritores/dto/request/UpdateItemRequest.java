package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateItemRequest(
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