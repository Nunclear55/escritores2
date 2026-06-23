package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
        @NotBlank
        @Size(min = 3, max = 150)
        String displayName,

        @Size(max = 5000)
        String bioText,

        @Size(max = 500)
        String avatarUrl
) {
}