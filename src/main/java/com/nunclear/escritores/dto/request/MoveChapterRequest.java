package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotNull;

public record MoveChapterRequest(
        @NotNull Integer targetVolumeId,
        @NotNull Integer newPositionIndex
) {
}