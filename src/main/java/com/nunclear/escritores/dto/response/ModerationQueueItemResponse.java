package com.nunclear.escritores.dto.response;

public record ModerationQueueItemResponse(
        Integer commentId,
        String reasonSummary
) {
}