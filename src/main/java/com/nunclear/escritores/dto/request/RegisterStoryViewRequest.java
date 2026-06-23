package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterStoryViewRequest(
        @NotNull Integer storyId,
        Integer chapterId,

        @Size(max = 100)
        String visitorToken,

        @Size(max = 45)
        String ipAddress,

        @Size(max = 500)
        String userAgentText
) {
}