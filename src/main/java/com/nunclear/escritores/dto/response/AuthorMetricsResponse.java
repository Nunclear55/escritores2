package com.nunclear.escritores.dto.response;

public record AuthorMetricsResponse(
        Integer userId,
        Long totalViews
) {
}