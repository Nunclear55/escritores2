package com.nunclear.escritores.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "mi-secreto-super-largo-para-tests-123456789");
        ReflectionTestUtils.setField(jwtService, "accessExpirationSeconds", 3600L);
    }

    @Test
    void generateAccessToken_deberiaGenerarTokenValido() {
        String token = jwtService.generateAccessToken(1, "juan", "user", "session-123");

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void extractAllClaims_deberiaRetornarClaimsCorrectos() {
        String token = jwtService.generateAccessToken(1, "juan", "user", "session-123");

        Claims claims = jwtService.extractAllClaims(token);

        assertEquals("1", claims.getSubject());
        assertEquals("juan", claims.get("username", String.class));
        assertEquals("user", claims.get("role", String.class));
        assertEquals("session-123", claims.get("sessionId", String.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    void extractUserId_deberiaRetornarUserId() {
        String token = jwtService.generateAccessToken(99, "ana", "admin", "abc");

        Integer userId = jwtService.extractUserId(token);

        assertEquals(99, userId);
    }

    @Test
    void isTokenValid_deberiaRetornarFalse_siTokenEsInvalido() {
        boolean valid = jwtService.isTokenValid("token-invalido");

        assertFalse(valid);
    }

    @Test
    void isTokenValid_deberiaRetornarFalse_siTokenExpirado() {
        ReflectionTestUtils.setField(jwtService, "accessExpirationSeconds", -10L);

        String token = jwtService.generateAccessToken(1, "juan", "user", "session-123");

        assertFalse(jwtService.isTokenValid(token));
    }
}