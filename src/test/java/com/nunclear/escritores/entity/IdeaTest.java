package com.nunclear.escritores.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.*;

class IdeaTest {

    @Test
    void prePersist_deberiaAsignarCreatedAtYUpdatedAt() {
        Idea idea = new Idea();
        idea.prePersist();
        assertNotNull(idea.getCreatedAt());
        assertNotNull(idea.getUpdatedAt());
        assertEquals(idea.getCreatedAt(), idea.getUpdatedAt());
    }

    @Test
    void preUpdate_deberiaActualizarUpdatedAt() {
        Idea idea = new Idea();
        idea.setUpdatedAt(LocalDateTime.of(2026, Month.APRIL, 22, 10, 0));
        idea.preUpdate();
        assertTrue(idea.getUpdatedAt().isAfter(LocalDateTime.of(2026, Month.APRIL, 22, 10, 0)));
    }
}
