package com.nunclear.escritores.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class StoryEventTest {

    @Test
    void prePersist_deberiaAsignarCreatedAtYUpdatedAt() {
        StoryEvent event = new StoryEvent();
        event.prePersist();
        assertNotNull(event.getCreatedAt());
        assertNotNull(event.getUpdatedAt());
        assertEquals(event.getCreatedAt(), event.getUpdatedAt());
    }

    @Test
    void preUpdate_deberiaActualizarUpdatedAt() {
        StoryEvent event = new StoryEvent();
        event.setUpdatedAt(LocalDateTime.of(2026, 4, 22, 10, 0));
        event.preUpdate();
        assertTrue(event.getUpdatedAt().isAfter(LocalDateTime.of(2026, 4, 22, 10, 0)));
    }
}