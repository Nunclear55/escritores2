package com.nunclear.escritores.dto.response;

public record CreateVolumeResponse(
        Integer id,
        Integer storyId,
        Integer arcId,
        String title,
        Integer positionIndex
) {
}