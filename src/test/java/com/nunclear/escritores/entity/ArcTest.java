package com.nunclear.escritores.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ArcTest {

    @Test
    void prePersist_deberiaAsignarCreatedAtYUpdatedAt() {
        Arc arc = new Arc();

        arc.prePersist();

        assertNotNull(arc.getCreatedAt());
        assertNotNull(arc.getUpdatedAt());
        assertEquals(arc.getCreatedAt(), arc.getUpdatedAt());
    }

    @Test
    void preUpdate_deberiaActualizarUpdatedAt() {
        Arc arc = new Arc();
        arc.setUpdatedAt(LocalDateTime.of(2026, 4, 22, 10, 0));

        arc.preUpdate();

        assertNotNull(arc.getUpdatedAt());
        assertTrue(arc.getUpdatedAt().isAfter(LocalDateTime.of(2026, 4, 22, 10, 0)));
    }
}