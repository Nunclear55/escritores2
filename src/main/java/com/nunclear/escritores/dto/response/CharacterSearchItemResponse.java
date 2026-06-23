package com.nunclear.escritores.dto.response;

public record CharacterSearchItemResponse(
        Integer id,
        String name,
        Integer storyId
) {
}