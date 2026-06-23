package com.nunclear.escritores.dto.response;

import java.time.LocalDateTime;

public record StoryPublicationResponse(
        Integer id,
        String publicationState,
        LocalDateTime publishedAt
) {
}