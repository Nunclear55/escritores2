package com.nunclear.escritores.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class StoryCharacterTest {

    @Test
    void prePersist_deberiaAsignarCreatedAtYUpdatedAt() {
        StoryCharacter character = new StoryCharacter();

        character.prePersist();

        assertNotNull(character.getCreatedAt());
        assertNotNull(character.getUpdatedAt());
        assertEquals(character.getCreatedAt(), character.getUpdatedAt());
    }

    @Test
    void preUpdate_deberiaActualizarUpdatedAt() {
        StoryCharacter character = new StoryCharacter();
        character.setUpdatedAt(LocalDateTime.of(2026, 4, 22, 10, 0));

        character.preUpdate();

        assertNotNull(character.getUpdatedAt());
        assertTrue(character.getUpdatedAt().isAfter(LocalDateTime.of(2026, 4, 22, 10, 0)));
    }
}