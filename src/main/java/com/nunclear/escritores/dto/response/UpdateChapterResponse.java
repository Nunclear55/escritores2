package com.nunclear.escritores.dto.response;

import java.time.LocalDateTime;

public record UpdateChapterResponse(
        Integer id,
        String title,
        LocalDateTime updatedAt
) {
}