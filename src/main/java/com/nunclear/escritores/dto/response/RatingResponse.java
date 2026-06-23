package com.nunclear.escritores.dto.response;

public record RatingResponse(
        Integer id,
        Integer storyId,
        Integer authorUserId,
        Integer scoreValue,
        String reviewText
) {
}