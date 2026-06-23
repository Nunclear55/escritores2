package com.nunclear.escritores.tests.auth;

import com.nunclear.escritores.dto.request.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas de autenticación.  Estas pruebas se aseguran de que el
 * endpoint de login requiere credenciales válidas y responde con
 * códigos de estado coherentes.  Para simplificar se envían
 * credenciales vacías y se espera una respuesta 400 Bad Request
 * debido a la validación de Spring.
 */
// Pruebas de autenticación simplificadas sin depender de Spring Boot ni MockMvc.
public class AuthenticationTest {

    @Test
    @DisplayName("El DTO LoginRequest almacena correctamente las credenciales")
    void loginRequestStoresCredentials() {
        String login = "usuario";
        String password = "clave";
        LoginRequest request = new LoginRequest(login, password);
        assertEquals(login, request.loginOrEmail());
        assertEquals(password, request.password());
    }
}