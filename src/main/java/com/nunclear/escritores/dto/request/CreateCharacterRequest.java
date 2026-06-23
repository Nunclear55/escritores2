package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateCharacterRequest(
        @NotNull Integer storyId,

        @NotBlank
        @Size(max = 255)
        String name,

        @Size(max = 5000)
        String description,

        @Size(max = 255)
        String characterRoleName,

        @Size(max = 255)
        String profession,

        @Size(max = 255)
        String ability,

        Integer age,
        LocalDate birthDate,
        Boolean isAlive,
        List<String> rolesJson,

        @Size(max = 500)
        String imageUrl
) {
}