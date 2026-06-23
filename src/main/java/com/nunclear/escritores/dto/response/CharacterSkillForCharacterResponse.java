package com.nunclear.escritores.dto.response;

public record CharacterSkillForCharacterResponse(
        Integer id,
        Integer skillId,
        String skillName,
        Integer proficiency,
        String notes
) {
}