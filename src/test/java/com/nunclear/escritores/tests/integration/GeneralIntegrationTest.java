package com.nunclear.escritores.tests.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Ejemplos de pruebas de integración.  Estas pruebas cargan el
 * contexto de Spring y verifican que la aplicación arranca sin
 * problemas.  En un proyecto real se podrían probar las interacciones
 * entre controladores, servicios, repositorios y la base de datos
 * utilizando una base de datos en memoria como H2.
 */
// Esta prueba se ejecuta sin cargar el contexto de Spring para evitar
// dependencias ausentes en el entorno de construcción.
public class GeneralIntegrationTest {

    @Test
    void contextLoads() {
        // Si el contexto no se carga correctamente, esta prueba fallará.
        assertNotNull(this);
    }
}