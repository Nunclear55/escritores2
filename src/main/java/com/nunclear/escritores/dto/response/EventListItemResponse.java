package com.nunclear.escritores.dto.response;

public record EventListItemResponse(
        Integer id,
        String title,
        Integer chapterId
) {
}