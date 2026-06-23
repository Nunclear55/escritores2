package com.nunclear.escritores.dto.response;

import java.time.LocalDateTime;

public record UpdateCharacterResponse(
        Integer id,
        String name,
        LocalDateTime updatedAt
) {
}