package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 100)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$")
        String loginName,

        @NotBlank
        @Email
        @Size(max = 255)
        String emailAddress,

        @NotBlank
        @Size(min = 3, max = 150)
        String displayName,

        @NotBlank
        @Size(min = 8, max = 100)
        String password
) {
}