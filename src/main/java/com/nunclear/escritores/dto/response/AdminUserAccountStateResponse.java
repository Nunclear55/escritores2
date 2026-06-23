package com.nunclear.escritores.dto.response;

import java.time.LocalDateTime;

public record AdminUserAccountStateResponse(
        Integer id,
        String accountState,
        LocalDateTime updatedAt
) {
}