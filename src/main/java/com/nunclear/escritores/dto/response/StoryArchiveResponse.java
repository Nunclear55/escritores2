package com.nunclear.escritores.dto.response;

import java.time.LocalDateTime;

public record StoryArchiveResponse(
        Integer id,
        LocalDateTime archivedAt
) {
}