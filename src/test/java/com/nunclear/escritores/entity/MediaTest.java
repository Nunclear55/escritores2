package com.nunclear.escritores.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MediaTest {

    @Test
    void prePersist_deberiaAsignarCreatedAtYUpdatedAt() {
        Media media = new Media();

        media.prePersist();

        assertNotNull(media.getCreatedAt());
        assertNotNull(media.getUpdatedAt());
        assertEquals(media.getCreatedAt(), media.getUpdatedAt());
    }

    @Test
    void preUpdate_deberiaActualizarUpdatedAt() {
        Media media = new Media();
        media.setUpdatedAt(LocalDateTime.of(2026, 4, 22, 10, 0));

        media.preUpdate();

        assertNotNull(media.getUpdatedAt());
        assertTrue(media.getUpdatedAt().isAfter(LocalDateTime.of(2026, 4, 22, 10, 0)));
    }
}