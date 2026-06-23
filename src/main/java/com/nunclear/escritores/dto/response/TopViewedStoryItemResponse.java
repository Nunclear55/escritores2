package com.nunclear.escritores.dto.response;

public record TopViewedStoryItemResponse(
        Integer storyId,
        String title,
        Long views
) {
}