package com.nunclear.escritores.dto.response;

public record CreateCharacterResponse(
        Integer id,
        Integer storyId,
        String name,
        String characterRoleName
) {
}