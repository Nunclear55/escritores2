package com.nunclear.escritores.dto.response;

import java.time.LocalDateTime;

public record AvatarResponse(
        String avatarUrl,
        LocalDateTime updatedAt
) {
}