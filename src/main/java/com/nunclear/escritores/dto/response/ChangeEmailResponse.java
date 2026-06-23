package com.nunclear.escritores.dto.response;

public record ChangeEmailResponse(
        String message,
        String pendingEmailAddress
) {
}