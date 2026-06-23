package com.nunclear.escritores.dto.response;

public record FavoriteResponse(
        Integer id,
        Integer storyId,
        Integer userId
) {
}
