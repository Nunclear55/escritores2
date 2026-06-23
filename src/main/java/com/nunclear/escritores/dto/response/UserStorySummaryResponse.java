package com.nunclear.escritores.dto.response;

public record UserStorySummaryResponse(
        Integer id,
        Integer ownerUserId,
        String title,
        String publicationState
) {
}