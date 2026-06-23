package com.nunclear.escritores.dto.response;

public record RatingListItemResponse(
        Integer id,
        Integer authorUserId,
        Integer scoreValue,
        String reviewText
) {
}