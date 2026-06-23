package com.nunclear.escritores.dto.response;

public record UserSearchItemResponse(
        Integer id,
        String loginName,
        String displayName,
        String avatarUrl
) {
}