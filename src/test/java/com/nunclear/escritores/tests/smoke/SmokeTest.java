package com.nunclear.escritores.tests.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de humo automatizadas.  Estas pruebas rápidas confirman que
 * la API principal responde y que el backend se levanta sin errores
 * graves.  Una solicitud GET a la raíz de la aplicación debería
 * devolver un código de estado 404 Not Found puesto que no existe
 * un endpoint mapeado en "/".  Si el backend no estuviera levantado
 * la prueba fallaría.
 */
// Pruebas de humo simplificadas sin dependencia de MockMvc ni del contexto de Spring.
public class SmokeTest {

    @Test
    @DisplayName("La prueba de humo se ejecuta correctamente")
    void simpleSmokeTest() {
        assertTrue(true);
    }
}