package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpsertRatingRequest(
        @NotNull Integer storyId,

        @NotNull
        @Min(1)
        @Max(5)
        Integer scoreValue,

        @Size(max = 5000)
        String reviewText
) {
}