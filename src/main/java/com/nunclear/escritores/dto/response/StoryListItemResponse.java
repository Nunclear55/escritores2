package com.nunclear.escritores.dto.response;

public record StoryListItemResponse(
        Integer id,
        String title,
        String slugText,
        String visibilityState,
        String publicationState
) {
}