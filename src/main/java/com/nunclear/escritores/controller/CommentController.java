package com.nunclear.escritores.controller;

import com.nunclear.escritores.dto.request.CreateCommentRequest;
import com.nunclear.escritores.dto.request.UpdateCommentRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // [107]
    @PostMapping
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public CreateCommentResponse createComment(@Valid @RequestBody CreateCommentRequest request) {
        return commentService.createComment(request);
    }

    // [108]
    @GetMapping("/{id}")
    public CommentDetailResponse getCommentById(@PathVariable Integer id) {
        return commentService.getCommentById(id);
    }

    // [109]
    @GetMapping("/story/{storyId}")
    public PageResponse<CommentListItemResponse> getCommentsByStory(
            @PathVariable Integer storyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return commentService.getCommentsByStory(storyId, page, size, sort);
    }

    // [110]
    @GetMapping("/chapter/{chapterId}")
    public PageResponse<CommentListItemResponse> getCommentsByChapter(
            @PathVariable Integer chapterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return commentService.getCommentsByChapter(chapterId, page, size, sort);
    }

    // [111]
    @GetMapping("/{id}/replies")
    public PageResponse<CommentReplyItemResponse> getReplies(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return commentService.getReplies(id, page, size, sort);
    }

    // [112]
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public UpdateCommentResponse updateComment(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        return commentService.updateComment(id, request);
    }

    // [113]
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public MessageResponse deleteComment(@PathVariable Integer id) {
        return commentService.deleteComment(id);
    }
}