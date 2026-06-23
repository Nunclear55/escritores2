package com.nunclear.escritores.dto.response;

public record CommentReplyItemResponse(
        Integer id,
        Integer parentCommentId,
        String content
) {
}