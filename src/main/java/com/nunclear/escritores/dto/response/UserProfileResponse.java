package com.nunclear.escritores.dto.response;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Integer id,
        String loginName,
        String displayName,
        String bioText,
        String avatarUrl,
        String accessLevel,
        LocalDateTime createdAt
) {
}