package com.nunclear.escritores.dto.response;

public record EventDetailResponse(
        Integer id,
        String title,
        String description,
        Integer importance,
        String eventKind
) {
}