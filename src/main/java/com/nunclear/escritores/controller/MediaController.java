package com.nunclear.escritores.controller;

import com.nunclear.escritores.dto.request.ReplaceMediaRequest;
import com.nunclear.escritores.dto.request.UploadMediaRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.service.MediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    // [101]
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public UploadMediaResponse uploadMedia(@Valid @RequestBody UploadMediaRequest request) {
        return mediaService.uploadMedia(request);
    }

    // [102]
    @GetMapping("/{id}")
    public MediaDetailResponse getMediaById(@PathVariable Integer id) {
        return mediaService.getMediaById(id);
    }

    // [103]
    @GetMapping("/chapter/{chapterId}")
    public PageResponse<MediaListItemResponse> getMediaByChapter(
            @PathVariable Integer chapterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return mediaService.getMediaByChapter(chapterId, page, size, sort);
    }

    // [104]
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public ReplaceMediaResponse replaceMedia(
            @PathVariable Integer id,
            @Valid @RequestBody ReplaceMediaRequest request
    ) {
        return mediaService.replaceMedia(id, request);
    }

    // [105]
    @GetMapping("/{id}/download")
    public MediaDownloadResponse downloadMedia(@PathVariable Integer id) {
        return mediaService.downloadMedia(id);
    }

    // [106]
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public MessageResponse deleteMedia(@PathVariable Integer id) {
        return mediaService.deleteMedia(id);
    }
}