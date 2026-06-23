package com.nunclear.escritores.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SkillTest {

    @Test
    void prePersist_deberiaAsignarCreatedAtYUpdatedAt() {
        Skill skill = new Skill();
        skill.prePersist();
        assertNotNull(skill.getCreatedAt());
        assertNotNull(skill.getUpdatedAt());
        assertEquals(skill.getCreatedAt(), skill.getUpdatedAt());
    }

    @Test
    void preUpdate_deberiaActualizarUpdatedAt() {
        Skill skill = new Skill();
        skill.setUpdatedAt(LocalDateTime.of(2026, 4, 22, 10, 0));
        skill.preUpdate();
        assertTrue(skill.getUpdatedAt().isAfter(LocalDateTime.of(2026, 4, 22, 10, 0)));
    }
}