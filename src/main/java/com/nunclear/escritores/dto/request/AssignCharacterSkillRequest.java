package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotNull;

public record AssignCharacterSkillRequest(
        @NotNull Integer storyCharacterId,
        @NotNull Integer skillId,
        Integer proficiency,
        String notes
) {
}