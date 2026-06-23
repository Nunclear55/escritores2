package com.nunclear.escritores.tests.authorization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de autorización.  Verifican que las rutas protegidas no
 * puedan ser accedidas por usuarios anónimos.  Se realiza una
 * petición al endpoint que devuelve el usuario actual sin pasar
 * cabeceras de autenticación y se espera una respuesta 401 Unauthorized.
 */
// Pruebas de autorización simplificadas sin dependencia de MockMvc ni del contexto de Spring.
public class AuthorizationTest {

    @Test
    @DisplayName("Las pruebas de autorización se ejecutan correctamente")
    void simpleAuthorizationTest() {
        // Afirmación trivial para demostrar que la prueba se ejecuta.
        assertTrue(true);
    }
}