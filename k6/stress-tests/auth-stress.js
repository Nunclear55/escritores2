import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para los endpoints de autenticación del controlador AuthController.
 *
 * Incluye un intento de inicio de sesión con credenciales ficticias y una
 * solicitud al endpoint protegido que devuelve información del usuario
 * autenticado.  Se consideran válidos los estados 200, 400, 401 y 403
 * para cubrir casos de credenciales incorrectas o falta de token.
 */

export const options = {
  stages: [
    { duration: '10s', target: 5 },
    { duration: '20s', target: 15 },
    { duration: '10s', target: 0 },
  ],
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  // Intentar iniciar sesión con datos ficticios
  const payload = JSON.stringify({ loginName: 'testuser', password: 'password123' });
  const params = { headers: { 'Content-Type': 'application/json' } };
  const resLogin = http.post(`${BASE_URL}/auth/login`, payload, params);
  check(resLogin, {
    'POST /auth/login estado aceptable': (r) => [200, 400, 401].includes(r.status),
  });

  // Obtener datos del usuario autenticado sin token
  const resMe = http.get(`${BASE_URL}/auth/me`);
  check(resMe, {
    'GET /auth/me status 401/403/200': (r) => [200, 401, 403].includes(r.status),
  });

  sleep(1);
}
