package com.nunclear.escritores.dto.response;

public record MediaListItemResponse(
        Integer id,
        String filename,
        String mediaKind
) {
}