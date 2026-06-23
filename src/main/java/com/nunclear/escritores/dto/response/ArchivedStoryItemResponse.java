package com.nunclear.escritores.dto.response;

import java.time.LocalDateTime;

public record ArchivedStoryItemResponse(
        Integer id,
        String title,
        LocalDateTime archivedAt
) {
}