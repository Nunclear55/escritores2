package com.nunclear.escritores.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.*;

class StoryTest {

    @Test
    void prePersist_deberiaAsignarCreatedAtYUpdatedAt() {
        Story story = new Story();

        story.prePersist();

        assertNotNull(story.getCreatedAt());
        assertNotNull(story.getUpdatedAt());
        assertEquals(story.getCreatedAt(), story.getUpdatedAt());
    }

    @Test
    void preUpdate_deberiaActualizarUpdatedAt() {
        Story story = new Story();
        story.setUpdatedAt(LocalDateTime.of(2026, Month.APRIL, 22, 10, 0));

        story.preUpdate();

        assertNotNull(story.getUpdatedAt());
        assertTrue(story.getUpdatedAt().isAfter(LocalDateTime.of(2026, Month.APRIL, 22, 10, 0)));
    }
}
