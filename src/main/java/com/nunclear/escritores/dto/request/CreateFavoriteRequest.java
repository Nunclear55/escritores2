package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateFavoriteRequest(
        @NotNull Integer storyId
) {
}