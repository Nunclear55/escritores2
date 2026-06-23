package com.nunclear.escritores.dto.response;

public record MyRatingDashboardItemResponse(
        Integer id,
        Integer storyId,
        Integer scoreValue
) {
}