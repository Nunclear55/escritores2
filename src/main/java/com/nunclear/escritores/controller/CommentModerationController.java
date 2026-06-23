package com.nunclear.escritores.controller;

import com.nunclear.escritores.dto.request.HideCommentRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.service.CommentModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/moderation/comments")
@RequiredArgsConstructor
public class CommentModerationController {

    private final CommentModerationService commentModerationService;

    // [114]
    @PostMapping("/{id}/hide")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public ModeratedCommentResponse hideComment(
            @PathVariable Integer id,
            @Valid @RequestBody HideCommentRequest request
    ) {
        return commentModerationService.hideComment(id, request);
    }

    // [115]
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public ModeratedCommentResponse restoreComment(@PathVariable Integer id) {
        return commentModerationService.restoreComment(id);
    }

    // [116]
    @GetMapping("/hidden")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public PageResponse<HiddenCommentItemResponse> getHiddenComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return commentModerationService.getHiddenComments(page, size, sort);
    }

    // [117]
    @GetMapping("/reported")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public PageResponse<ReportedCommentItemResponse> getReportedComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return commentModerationService.getReportedComments(page, size);
    }

    // [118]
    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public PageResponse<ModerationQueueItemResponse> getModerationQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return commentModerationService.getModerationQueue(page, size);
    }
}