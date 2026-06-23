package com.nunclear.escritores.dto.response;

public record FollowResponse(
        Integer id,
        Integer followerUserId,
        Integer followedUserId
) {
}