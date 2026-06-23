package com.nunclear.escritores.dto.response;

import java.time.LocalDateTime;

public record ChapterArchiveResponse(
        Integer id,
        LocalDateTime archivedAt
) {
}