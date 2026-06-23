package com.nunclear.escritores.tests.regression;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Pruebas de regresión automatizadas.  Este tipo de pruebas se
 * ejecutan después de realizar cambios en el código para confirmar
 * que las funcionalidades existentes siguen funcionando.  Aquí
 * simplemente se utiliza assertDoesNotThrow para ilustrar el patrón
 * de verificar que una operación no lance excepciones.  En un
 * proyecto real se reutilizarían las pruebas unitarias e integradas
 * existentes como pruebas de regresión.
 */
public class RegressionTest {

    @Test
    @DisplayName("Operación simple no lanza excepción")
    void noException() {
        assertDoesNotThrow(() -> {
            // Alguna lógica simple que antes funcionaba correctamente.
            int resultado = 1 + 1;
            if (resultado != 2) {
                throw new IllegalStateException("La aritmética básica ha fallado");
            }
        });
    }
}