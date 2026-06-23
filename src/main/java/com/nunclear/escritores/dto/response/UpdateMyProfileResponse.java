package com.nunclear.escritores.dto.response;

import java.time.LocalDateTime;

public record UpdateMyProfileResponse(
        Integer id,
        String displayName,
        String bioText,
        String avatarUrl,
        LocalDateTime updatedAt
) {
}