package com.nunclear.escritores.dto.response;

public record StoryDetailResponse(
        Integer id,
        Integer ownerUserId,
        String title,
        String slugText,
        String description,
        String visibilityState,
        String publicationState
) {
}