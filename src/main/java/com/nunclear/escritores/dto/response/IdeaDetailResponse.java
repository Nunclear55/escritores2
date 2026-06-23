package com.nunclear.escritores.dto.response;

public record IdeaDetailResponse(
        Integer id,
        Integer storyId,
        String title,
        String content
) {
}