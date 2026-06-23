package com.nunclear.escritores.dto.response;

public record VolumeDetailResponse(
        Integer id,
        Integer storyId,
        Integer arcId,
        String title,
        Integer positionIndex
) {
}