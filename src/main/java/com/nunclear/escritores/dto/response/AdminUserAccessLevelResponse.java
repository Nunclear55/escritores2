package com.nunclear.escritores.dto.response;

import java.time.LocalDateTime;

public record AdminUserAccessLevelResponse(
        Integer id,
        String accessLevel,
        LocalDateTime updatedAt
) {
}