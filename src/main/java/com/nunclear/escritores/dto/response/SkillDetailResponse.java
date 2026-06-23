package com.nunclear.escritores.dto.response;

public record SkillDetailResponse(
        Integer id,
        Integer storyId,
        String name,
        String categoryName,
        Integer levelValue
) {
}