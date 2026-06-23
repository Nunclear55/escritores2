package com.nunclear.escritores.dto.response;

public record CommentDetailResponse(
        Integer id,
        Integer storyId,
        Integer chapterId,
        Integer authorUserId,
        String content,
        String visibilityState
) {
}