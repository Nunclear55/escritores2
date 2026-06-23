package com.nunclear.escritores.dto.response;

public record CommentListItemResponse(
        Integer id,
        String content,
        Integer authorUserId
) {
}