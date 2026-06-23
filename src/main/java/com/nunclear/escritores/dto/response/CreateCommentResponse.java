package com.nunclear.escritores.dto.response;

public record CreateCommentResponse(
        Integer id,
        Integer storyId,
        Integer authorUserId,
        String content,
        String visibilityState
) {
}