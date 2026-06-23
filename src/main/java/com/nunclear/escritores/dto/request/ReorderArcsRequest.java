package com.nunclear.escritores.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReorderArcsRequest(
        @NotNull Integer storyId,
        @NotEmpty List<@Valid ReorderArcItemRequest> items
) {
}