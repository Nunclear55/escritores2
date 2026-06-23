package com.nunclear.escritores.dto.response;

public record CharacterListItemResponse(
        Integer id,
        String name,
        String characterRoleName
) {
}