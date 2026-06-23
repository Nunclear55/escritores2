package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotNull;

public record ReorderChapterItemRequest(
        @NotNull Integer chapterId,
        @NotNull Integer positionIndex
) {
}