package com.nunclear.escritores.controller;

import com.nunclear.escritores.dto.request.*;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.service.ChapterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chapters")
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;

    // [41]
    @PostMapping
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public CreateChapterResponse createChapter(@Valid @RequestBody CreateChapterRequest request) {
        return chapterService.createChapter(request);
    }

    // [42]
    @GetMapping("/{id}")
    public ChapterDetailResponse getChapterById(@PathVariable Integer id) {
        return chapterService.getChapterById(id);
    }

    // [43]
    @GetMapping("/story/{storyId}")
    public PageResponse<ChapterListItemResponse> getChaptersByStory(
            @PathVariable Integer storyId,
            @RequestParam(defaultValue = "false") boolean includeDrafts,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return chapterService.getChaptersByStory(storyId, includeDrafts, page, size, sort);
    }

    // [44]
    @GetMapping("/story/{storyId}/published")
    public PageResponse<ChapterListItemResponse> getPublishedChaptersByStory(
            @PathVariable Integer storyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return chapterService.getPublishedChaptersByStory(storyId, page, size, sort);
    }

    // [45]
    @GetMapping("/me/drafts")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public PageResponse<ChapterListItemResponse> getMyDrafts(
            @RequestParam(required = false) Integer storyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return chapterService.getMyDrafts(storyId, page, size, sort);
    }

    // [46]
    @GetMapping("/search")
    public PageResponse<ChapterSearchItemResponse> searchChapters(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer storyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return chapterService.searchChapters(q, storyId, page, size, sort);
    }

    // [47]
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public UpdateChapterResponse updateChapter(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateChapterRequest request
    ) {
        return chapterService.updateChapter(id, request);
    }

    // [48]
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public ChapterPublicationStateResponse publishChapter(@PathVariable Integer id) {
        return chapterService.publishChapter(id);
    }

    // [49]
    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public ChapterPublicationStateResponse unpublishChapter(@PathVariable Integer id) {
        return chapterService.unpublishChapter(id);
    }

    // [50]
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public ChapterArchiveResponse archiveChapter(@PathVariable Integer id) {
        return chapterService.archiveChapter(id);
    }

    // [51]
    @PostMapping("/reorder")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public MessageResponse reorderChapters(@Valid @RequestBody ReorderChaptersRequest request) {
        return chapterService.reorderChapters(request);
    }

    // [52]
    @PostMapping("/{id}/move")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public MoveChapterResponse moveChapter(
            @PathVariable Integer id,
            @Valid @RequestBody MoveChapterRequest request
    ) {
        return chapterService.moveChapter(id, request);
    }

    // [53]
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public MessageResponse deleteChapter(@PathVariable Integer id) {
        return chapterService.deleteChapter(id);
    }
}