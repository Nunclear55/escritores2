package com.nunclear.escritores.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CharacterSkillTest {

    @Test
    void prePersist_deberiaAsignarCreatedAtYUpdatedAt() {
        CharacterSkill relation = new CharacterSkill();
        relation.prePersist();
        assertNotNull(relation.getCreatedAt());
        assertNotNull(relation.getUpdatedAt());
        assertEquals(relation.getCreatedAt(), relation.getUpdatedAt());
    }

    @Test
    void preUpdate_deberiaActualizarUpdatedAt() {
        CharacterSkill relation = new CharacterSkill();
        relation.setUpdatedAt(LocalDateTime.of(2026, 4, 22, 10, 0));
        relation.preUpdate();
        assertTrue(relation.getUpdatedAt().isAfter(LocalDateTime.of(2026, 4, 22, 10, 0)));
    }
}