package com.nunclear.escritores.dto.response;

import java.time.LocalDateTime;

public record UpdateStoryResponse(
        Integer id,
        String title,
        LocalDateTime updatedAt
) {
}