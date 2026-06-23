package com.nunclear.escritores.dto.response;

import java.time.LocalDateTime;

public record RecentCommentDashboardItemResponse(
        Integer id,
        String content,
        LocalDateTime createdAt
) {
}