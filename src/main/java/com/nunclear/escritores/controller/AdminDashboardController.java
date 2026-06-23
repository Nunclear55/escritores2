package com.nunclear.escritores.controller;

import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    // [172]
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminDashboardSummaryResponse getAdminSummary() {
        return dashboardService.getAdminSummary();
    }

    // [173]
    @GetMapping("/activity")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public PageResponse<SystemActivityItemResponse> getActivity(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return dashboardService.getSystemActivity(page, size);
    }
}