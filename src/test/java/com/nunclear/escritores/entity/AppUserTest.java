package com.nunclear.escritores.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AppUserTest {

    @Test
    void prePersist_deberiaAsignarCreatedAtYUpdatedAt() {
        AppUser user = new AppUser();

        user.prePersist();

        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
        assertEquals(user.getCreatedAt(), user.getUpdatedAt());
    }

    @Test
    void preUpdate_deberiaActualizarUpdatedAt() {
        AppUser user = new AppUser();
        user.setUpdatedAt(LocalDateTime.of(2026, 4, 22, 10, 0));

        user.preUpdate();

        assertNotNull(user.getUpdatedAt());
        assertTrue(user.getUpdatedAt().isAfter(LocalDateTime.of(2026, 4, 22, 10, 0)));
    }
}