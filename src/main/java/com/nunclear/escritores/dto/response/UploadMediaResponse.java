package com.nunclear.escritores.dto.response;

public record UploadMediaResponse(
        Integer id,
        String filename,
        String originalFilename,
        String mediaKind,
        Integer chapterId
) {
}