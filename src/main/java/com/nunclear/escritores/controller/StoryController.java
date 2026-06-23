package com.nunclear.escritores.controller;

import com.nunclear.escritores.dto.request.CreateStoryRequest;
import com.nunclear.escritores.dto.request.DuplicateStoryRequest;
import com.nunclear.escritores.dto.request.UpdateStoryRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.service.StoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;

    // [26]
    @PostMapping
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public CreateStoryResponse createStory(@Valid @RequestBody CreateStoryRequest request) {
        return storyService.createStory(request);
    }

    // [27]
    @GetMapping("/{id}")
    public StoryDetailResponse getStoryById(@PathVariable Integer id) {
        return storyService.getStoryById(id);
    }

    // [28]
    @GetMapping("/slug/{slug}")
    public StorySlugResponse getStoryBySlug(@PathVariable String slug) {
        return storyService.getStoryBySlug(slug);
    }

    // [29]
    @GetMapping
    public PageResponse<StoryListItemResponse> listPublicStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return storyService.listPublicStories(page, size, sort);
    }

    // [30]
    @GetMapping("/search")
    public PageResponse<StoryListItemResponse> searchStories(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String visibilityState,
            @RequestParam(required = false) String publicationState,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return storyService.searchStories(q, visibilityState, publicationState, page, size, sort);
    }

    // [31]
    @GetMapping("/user/{userId}")
    public PageResponse<UserStorySummaryResponse> getStoriesByUser(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "false") boolean includeDrafts,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return storyService.getStoriesByUser(userId, includeDrafts, page, size, sort);
    }

    // [32]
    @GetMapping("/me/drafts")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public PageResponse<UserStorySummaryResponse> getMyDrafts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return storyService.getMyDrafts(page, size, sort);
    }

    // [33]
    @GetMapping("/me/archived")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public PageResponse<ArchivedStoryItemResponse> getMyArchived(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return storyService.getMyArchived(page, size, sort);
    }

    // [34]
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public UpdateStoryResponse updateStory(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateStoryRequest request
    ) {
        return storyService.updateStory(id, request);
    }

    // [35]
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public StoryPublicationResponse publishStory(@PathVariable Integer id) {
        return storyService.publishStory(id);
    }

    // [36]
    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public StoryPublicationResponse unpublishStory(@PathVariable Integer id) {
        return storyService.unpublishStory(id);
    }

    // [37]
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public StoryArchiveResponse archiveStory(@PathVariable Integer id) {
        return storyService.archiveStory(id);
    }

    // [38]
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public StoryArchiveResponse restoreStory(@PathVariable Integer id) {
        return storyService.restoreStory(id);
    }

    // [39]
    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public DuplicateStoryResponse duplicateStory(
            @PathVariable Integer id,
            @Valid @RequestBody DuplicateStoryRequest request
    ) {
        return storyService.duplicateStory(id, request);
    }

    // [40]
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public MessageResponse deleteStory(@PathVariable Integer id) {
        return storyService.deleteStory(id);
    }
}