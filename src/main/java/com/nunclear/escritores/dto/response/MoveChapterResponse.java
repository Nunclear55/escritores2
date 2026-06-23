package com.nunclear.escritores.dto.response;

public record MoveChapterResponse(
        Integer id,
        Integer volumeId,
        Integer positionIndex
) {
}