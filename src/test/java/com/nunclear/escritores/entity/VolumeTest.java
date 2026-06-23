package com.nunclear.escritores.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class VolumeTest {

    @Test
    void prePersist_deberiaAsignarCreatedAtYUpdatedAt() {
        Volume volume = new Volume();

        volume.prePersist();

        assertNotNull(volume.getCreatedAt());
        assertNotNull(volume.getUpdatedAt());
        assertEquals(volume.getCreatedAt(), volume.getUpdatedAt());
    }

    @Test
    void preUpdate_deberiaActualizarUpdatedAt() {
        Volume volume = new Volume();
        volume.setUpdatedAt(LocalDateTime.of(2026, 4, 22, 10, 0));

        volume.preUpdate();

        assertNotNull(volume.getUpdatedAt());
        assertTrue(volume.getUpdatedAt().isAfter(LocalDateTime.of(2026, 4, 22, 10, 0)));
    }
}