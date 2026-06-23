package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record UpdateEventRequest(
        @NotBlank
        @Size(max = 255)
        String title,

        @Size(max = 5000)
        String description,

        LocalDate eventOn,
        Integer importance,

        @Size(max = 100)
        String eventKind,

        List<String> tagsJson,
        List<Integer> linkedCharacterIds
) {
}