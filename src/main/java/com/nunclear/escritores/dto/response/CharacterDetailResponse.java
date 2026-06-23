package com.nunclear.escritores.dto.response;

public record CharacterDetailResponse(
        Integer id,
        Integer storyId,
        String name,
        String description,
        String characterRoleName,
        String profession
) {
}