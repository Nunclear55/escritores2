package com.nunclear.escritores.dto.response;

public record RatingAverageResponse(
        Double averageScore,
        Long ratingsCount
) {
}