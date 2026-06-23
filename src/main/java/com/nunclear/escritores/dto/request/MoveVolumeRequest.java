package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotNull;

public record MoveVolumeRequest(
        @NotNull Integer targetArcId,
        @NotNull Integer newPositionIndex
) {
}