package com.nunclear.escritores.dto.response;

public record ReportedCommentItemResponse(
        Integer id,
        long reportsCount,
        String content
) {
}