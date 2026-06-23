package com.nunclear.escritores.dto.response;

public record AdminUserByRoleItemResponse(
        Integer id,
        String loginName,
        String accessLevel
) {
}