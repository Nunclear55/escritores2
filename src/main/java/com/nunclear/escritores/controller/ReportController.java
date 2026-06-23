package com.nunclear.escritores.controller;

import com.nunclear.escritores.dto.request.*;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // [125]
    @PostMapping("/story")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public ReportTargetResponse reportStory(@Valid @RequestBody CreateStoryReportRequest request) {
        return reportService.reportStory(request);
    }

    // [126]
    @PostMapping("/chapter")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public ReportTargetResponse reportChapter(@Valid @RequestBody CreateChapterReportRequest request) {
        return reportService.reportChapter(request);
    }

    // [127]
    @PostMapping("/comment")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public ReportTargetResponse reportComment(@Valid @RequestBody CreateCommentReportRequest request) {
        return reportService.reportComment(request);
    }

    // [128]
    @PostMapping("/user")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public ReportTargetResponse reportUser(@Valid @RequestBody CreateUserReportRequest request) {
        return reportService.reportUser(request);
    }

    // [129]
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public PageResponse<ReportListItemResponse> getPendingReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return reportService.getPendingReports(page, size, sort);
    }

    // [130]
    @GetMapping
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public PageResponse<ReportListItemResponse> getReportsByStatus(
            @RequestParam String statusName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return reportService.getReportsByStatus(statusName, page, size, sort);
    }

    // [131]
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public ReportDetailResponse getReportById(@PathVariable Integer id) {
        return reportService.getReportById(id);
    }

    // [132]
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public AssignedReportResponse assignReviewer(
            @PathVariable Integer id,
            @Valid @RequestBody AssignReportReviewerRequest request
    ) {
        return reportService.assignReviewer(id, request);
    }

    // [133]
    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public ResolvedReportResponse reviewReport(
            @PathVariable Integer id,
            @Valid @RequestBody ReviewReportRequest request
    ) {
        return reportService.reviewReport(id, request);
    }

    // [134]
    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public ResolvedReportResponse resolveReport(
            @PathVariable Integer id,
            @Valid @RequestBody ResolveReportRequest request
    ) {
        return reportService.resolveReport(id, request);
    }

    // [135]
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public ResolvedReportResponse rejectReport(
            @PathVariable Integer id,
            @Valid @RequestBody ResolveReportRequest request
    ) {
        return reportService.rejectReport(id, request);
    }

    // [136]
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public PageResponse<ReportListItemResponse> getHistory(
            @RequestParam(required = false) Integer targetUserId,
            @RequestParam(required = false) Integer storyId,
            @RequestParam(required = false) Integer commentId,
            @RequestParam(required = false) Integer chapterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return reportService.getHistory(targetUserId, storyId, commentId, chapterId, page, size, sort);
    }
}