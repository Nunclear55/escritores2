package com.nunclear.escritores.dto.response;

public record ReportTargetResponse(
        Integer id,
        Integer storyId,
        Integer chapterId,
        Integer commentId,
        Integer targetUserId,
        String statusName
) {
}