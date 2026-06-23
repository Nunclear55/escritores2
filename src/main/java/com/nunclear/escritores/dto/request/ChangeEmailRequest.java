package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeEmailRequest(
        @NotBlank
        @Email
        @Size(max = 255)
        String newEmailAddress,

        @NotBlank
        String password
) {
}