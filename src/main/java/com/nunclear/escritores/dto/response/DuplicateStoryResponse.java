package com.nunclear.escritores.dto.response;

public record DuplicateStoryResponse(
        Integer id,
        Integer sourceStoryId,
        String title,
        String publicationState
) {
}