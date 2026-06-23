package com.nunclear.escritores.dto.response;

public record ItemDetailResponse(
        Integer id,
        Integer storyId,
        String name,
        String description,
        Integer quantity,
        String unitName
) {
}