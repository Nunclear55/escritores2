package com.nunclear.escritores.dto.response;

public record CreateChapterResponse(
        Integer id,
        Integer storyId,
        String title,
        String publicationState,
        Integer readingMinutes,
        Integer wordCount
) {
}