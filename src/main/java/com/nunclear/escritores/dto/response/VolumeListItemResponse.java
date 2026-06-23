package com.nunclear.escritores.dto.response;

public record VolumeListItemResponse(
        Integer id,
        String title,
        Integer positionIndex
) {
}