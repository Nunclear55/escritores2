package com.nunclear.escritores.dto.response;

public record SanctionResponse(
        Integer id,
        Integer targetUserId,
        String sanctionKind,
        Boolean isActive
) {
}