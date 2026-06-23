package com.nunclear.escritores.controller;

import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // [169]
    @GetMapping("/me/summary")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public MyDashboardSummaryResponse getMySummary() {
        return dashboardService.getMySummary();
    }

    // [170]
    @GetMapping("/me/recent-comments")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public PageResponse<RecentCommentDashboardItemResponse> getMyRecentComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort
    ) {
        return dashboardService.getMyRecentComments(page, size, sort);
    }

    // [171]
    @GetMapping("/me/ratings")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public PageResponse<MyRatingDashboardItemResponse> getMyRatings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort
    ) {
        return dashboardService.getMyRatings(page, size, sort);
    }
}