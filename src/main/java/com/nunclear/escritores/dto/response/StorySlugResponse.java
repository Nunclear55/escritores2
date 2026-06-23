package com.nunclear.escritores.dto.response;

public record StorySlugResponse(
        Integer id,
        String slugText,
        String title
) {
}