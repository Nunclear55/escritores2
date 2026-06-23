package com.nunclear.escritores.dto.response;

public record CreateArcResponse(
        Integer id,
        Integer storyId,
        String title,
        Integer positionIndex
) {
}