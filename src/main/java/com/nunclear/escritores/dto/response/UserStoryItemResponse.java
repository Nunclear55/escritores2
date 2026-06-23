package com.nunclear.escritores.dto.response;

public record UserStoryItemResponse(
        Integer id,
        String title,
        String slugText,
        String publicationState,
        String visibilityState
) {
}