package com.nunclear.escritores.dto.response;

public record ArcListItemResponse(
        Integer id,
        String title,
        Integer positionIndex
) {
}