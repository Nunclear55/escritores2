package com.nunclear.escritores.dto.response;

public record CreateSkillResponse(
        Integer id,
        Integer storyId,
        String name,
        String categoryName,
        Integer levelValue
) {
}