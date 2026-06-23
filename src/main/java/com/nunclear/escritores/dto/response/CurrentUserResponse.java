package com.nunclear.escritores.dto.response;

public record CurrentUserResponse(
        Integer id,
        String loginName,
        String emailAddress,
        String displayName,
        String bioText,
        String avatarUrl,
        String accessLevel,
        String accountState
) {
}