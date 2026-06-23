package com.nunclear.escritores.dto.response;

public record CharacterSkillForSkillResponse(
        Integer id,
        Integer storyCharacterId,
        String characterName,
        Integer proficiency
) {
}