package com.nunclear.escritores.dto.response;

public record ArcDetailResponse(
        Integer id,
        Integer storyId,
        String title,
        String subtitle,
        Integer positionIndex
) {
}