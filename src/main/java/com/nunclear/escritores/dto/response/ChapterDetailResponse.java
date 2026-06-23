package com.nunclear.escritores.dto.response;

public record ChapterDetailResponse(
        Integer id,
        Integer storyId,
        String title,
        String content,
        String publicationState,
        Integer wordCount
) {
}