package com.nunclear.escritores.dto.response;

public record SanctionListItemResponse(
        Integer id,
        Integer targetUserId,
        String sanctionKind,
        Boolean isActive
) {
}