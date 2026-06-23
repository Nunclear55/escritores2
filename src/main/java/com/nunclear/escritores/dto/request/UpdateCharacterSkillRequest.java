package com.nunclear.escritores.dto.request;

public record UpdateCharacterSkillRequest(
        Integer proficiency,
        String notes
) {
}