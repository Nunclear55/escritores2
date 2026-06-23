package com.nunclear.escritores.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ItemTest {

    @Test
    void prePersist_deberiaAsignarCreatedAtYUpdatedAt() {
        Item item = new Item();
        item.prePersist();
        assertNotNull(item.getCreatedAt());
        assertNotNull(item.getUpdatedAt());
        assertEquals(item.getCreatedAt(), item.getUpdatedAt());
    }

    @Test
    void preUpdate_deberiaActualizarUpdatedAt() {
        Item item = new Item();
        item.setUpdatedAt(LocalDateTime.of(2026, 4, 22, 10, 0));
        item.preUpdate();
        assertTrue(item.getUpdatedAt().isAfter(LocalDateTime.of(2026, 4, 22, 10, 0)));
    }
}