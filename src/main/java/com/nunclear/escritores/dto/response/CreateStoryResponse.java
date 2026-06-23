package com.nunclear.escritores.dto.response;

import java.time.LocalDateTime;

public record CreateStoryResponse(
        Integer id,
        Integer ownerUserId,
        String title,
        String slugText,
        String visibilityState,
        String publicationState,
        LocalDateTime createdAt
) {
}