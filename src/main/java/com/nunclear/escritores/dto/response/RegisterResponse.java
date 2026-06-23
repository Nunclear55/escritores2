package com.nunclear.escritores.dto.response;

import java.time.LocalDateTime;

public record RegisterResponse(
        Integer id,
        String loginName,
        String emailAddress,
        String displayName,
        String accessLevel,
        String accountState,
        LocalDateTime createdAt
) {
}