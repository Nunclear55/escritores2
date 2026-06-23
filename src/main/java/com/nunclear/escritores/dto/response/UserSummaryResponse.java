package com.nunclear.escritores.dto.response;

public record UserSummaryResponse(
        Integer id,
        String loginName,
        String displayName,
        String accessLevel
) {
}