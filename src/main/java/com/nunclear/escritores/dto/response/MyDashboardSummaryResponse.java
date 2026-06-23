package com.nunclear.escritores.dto.response;

public record MyDashboardSummaryResponse(
        long storiesCount,
        long draftStoriesCount,
        long favoritesCount,
        long followingCount,
        long recentCommentsCount
) {
}