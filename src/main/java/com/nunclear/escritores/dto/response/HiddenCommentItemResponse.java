package com.nunclear.escritores.dto.response;

public record HiddenCommentItemResponse(
        Integer id,
        String visibilityState,
        String content
) {
}