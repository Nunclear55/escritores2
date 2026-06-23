package com.nunclear.escritores.dto.response;

public record AssignCharacterSkillResponse(
        Integer id,
        Integer storyCharacterId,
        Integer skillId,
        Integer proficiency,
        String notes
) {
}