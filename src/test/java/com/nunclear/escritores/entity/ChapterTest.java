package com.nunclear.escritores.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ChapterTest {

    @Test
    void prePersist_deberiaAsignarCreatedAtYUpdatedAt() {
        Chapter chapter = new Chapter();

        chapter.prePersist();

        assertNotNull(chapter.getCreatedAt());
        assertNotNull(chapter.getUpdatedAt());
        assertEquals(chapter.getCreatedAt(), chapter.getUpdatedAt());
    }

    @Test
    void preUpdate_deberiaActualizarUpdatedAt() {
        Chapter chapter = new Chapter();
        chapter.setUpdatedAt(LocalDateTime.of(2026, 4, 22, 10, 0));

        chapter.preUpdate();

        assertNotNull(chapter.getUpdatedAt());
        assertTrue(chapter.getUpdatedAt().isAfter(LocalDateTime.of(2026, 4, 22, 10, 0)));
    }


}