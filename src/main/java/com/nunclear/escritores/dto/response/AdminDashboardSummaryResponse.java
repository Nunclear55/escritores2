package com.nunclear.escritores.dto.response;

public record AdminDashboardSummaryResponse(
        long usersCount,
        long storiesCount,
        long pendingReportsCount,
        long activeSanctionsCount,
        long activeNoticesCount
) {
}