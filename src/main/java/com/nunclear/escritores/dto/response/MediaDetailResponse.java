package com.nunclear.escritores.dto.response;

public record MediaDetailResponse(
        Integer id,
        String filename,
        String originalFilename,
        String mediaKind,
        String description,
        Integer chapterId,
        String storagePath
) {
}