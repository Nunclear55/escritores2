package com.nunclear.escritores.dto.response;

public record StoryMetricsResponse(
        Integer storyId,
        Long views,
        Long favorites,
        Long ratingsCount,
        Double averageScore
) {
}