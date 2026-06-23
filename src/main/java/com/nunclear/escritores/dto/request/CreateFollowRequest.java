package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateFollowRequest(
        @NotNull Integer followedUserId
) {
}