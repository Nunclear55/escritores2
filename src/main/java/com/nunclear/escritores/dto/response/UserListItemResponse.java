package com.nunclear.escritores.dto.response;

public record UserListItemResponse(
        Integer id,
        String loginName,
        String displayName,
        String accessLevel,
        String accountState
) {
}