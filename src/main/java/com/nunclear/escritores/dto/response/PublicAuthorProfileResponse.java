package com.nunclear.escritores.dto.response;

public record PublicAuthorProfileResponse(
        Integer id,
        String displayName,
        String bioText,
        String avatarUrl,
        long followersCount,
        long storiesCount
) {
}