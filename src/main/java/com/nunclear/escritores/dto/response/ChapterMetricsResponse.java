package com.nunclear.escritores.dto.response;

public record ChapterMetricsResponse(
        Integer chapterId,
        Long views,
        Long commentsCount
) {
}