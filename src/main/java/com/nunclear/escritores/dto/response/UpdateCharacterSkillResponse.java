package com.nunclear.escritores.dto.response;

public record UpdateCharacterSkillResponse(
        Integer id,
        Integer proficiency,
        String notes
) {
}