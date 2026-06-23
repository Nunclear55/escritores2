package com.nunclear.escritores.dto.response;

import java.time.LocalDateTime;

public record UpdateCommentResponse(
        Integer id,
        String content,
        LocalDateTime editedAt
) {
}