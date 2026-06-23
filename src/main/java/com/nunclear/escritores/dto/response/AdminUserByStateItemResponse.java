package com.nunclear.escritores.dto.response;

public record AdminUserByStateItemResponse(
        Integer id,
        String loginName,
        String accountState
) {
}