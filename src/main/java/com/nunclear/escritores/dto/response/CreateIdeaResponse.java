package com.nunclear.escritores.dto.response;

public record CreateIdeaResponse(
        Integer id,
        Integer storyId,
        String title
) {
}