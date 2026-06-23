package com.nunclear.escritores.dto.response;

public record CreateEventResponse(
        Integer id,
        Integer storyId,
        Integer chapterId,
        String title
) {
}