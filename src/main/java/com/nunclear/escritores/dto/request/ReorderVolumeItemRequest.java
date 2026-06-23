package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotNull;

public record ReorderVolumeItemRequest(
        @NotNull Integer volumeId,
        @NotNull Integer positionIndex
) {
}