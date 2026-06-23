package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotNull;

public record ReorderArcItemRequest(
        @NotNull Integer arcId,
        @NotNull Integer positionIndex
) {
}