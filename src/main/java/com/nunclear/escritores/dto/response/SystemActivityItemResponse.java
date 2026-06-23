package com.nunclear.escritores.dto.response;

import java.time.LocalDateTime;

public record SystemActivityItemResponse(
        String type,
        Integer referenceId,
        LocalDateTime createdAt
) {
}