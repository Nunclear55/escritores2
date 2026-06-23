package com.nunclear.escritores.dto.response;

public record CreateItemResponse(
        Integer id,
        Integer storyId,
        String name
) {
}