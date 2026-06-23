package com.nunclear.escritores.dto.response;

public record ReportListItemResponse(
        Integer id,
        Integer storyId,
        Integer chapterId,
        Integer commentId,
        Integer targetUserId,
        String statusName
) {
}