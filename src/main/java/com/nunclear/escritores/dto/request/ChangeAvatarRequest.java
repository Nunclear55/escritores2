package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeAvatarRequest(
        @NotBlank
        @Size(max = 500)
        String avatarUrl
) {
}