package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSkillRequest(
        @NotBlank
        @Size(max = 255)
        String name,

        @Size(max = 5000)
        String description,

        @Size(max = 100)
        String categoryName,

        Integer levelValue
) {
}