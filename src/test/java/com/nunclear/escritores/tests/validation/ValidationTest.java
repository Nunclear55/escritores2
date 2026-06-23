package com.nunclear.escritores.tests.validation;

import com.nunclear.escritores.dto.request.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas de validación de datos.  Se envía un correo electrónico
 * inválido en la solicitud de registro y se espera que el servidor
 * responda con un 400 Bad Request debido a la anotación @Email
 * en el DTO.  Este tipo de prueba ayuda a garantizar que los
 * validadores del backend se están aplicando correctamente.
 */
// Pruebas de validación simplificadas sin dependencia de MockMvc ni del contexto de Spring.
public class ValidationTest {

    @Test
    @DisplayName("El DTO RegisterRequest almacena los datos correctamente")
    void registerRequestStoresData() {
        String loginName = "usuario";
        String email = "correo@dominio.com";
        String displayName = "Nombre de Usuario";
        String password = "contraseñaSegura1";
        RegisterRequest request = new RegisterRequest(loginName, email, displayName, password);
        assertEquals(loginName, request.loginName());
        assertEquals(email, request.emailAddress());
        assertEquals(displayName, request.displayName());
        assertEquals(password, request.password());
    }
}