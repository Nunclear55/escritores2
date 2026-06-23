package com.nunclear.escritores.dto.response;

public record ReportDetailResponse(
        Integer id,
        Integer reporterUserId,
        Integer storyId,
        Integer chapterId,
        Integer commentId,
        Integer targetUserId,
        String reasonText,
        String statusName,
        Integer reviewedByUserId,
        String resolutionText
) {
}