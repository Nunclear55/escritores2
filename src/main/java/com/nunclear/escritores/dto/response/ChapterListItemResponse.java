package com.nunclear.escritores.dto.response;

import java.time.LocalDateTime;

public record ChapterListItemResponse(
        Integer id,
        String title,
        Integer positionIndex,
        String publicationState,
        LocalDateTime archivedAt
) {
}