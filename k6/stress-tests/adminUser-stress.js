import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para los endpoints de AdminUserController.
 *
 * Simula solicitudes concurrentes a los endpoints de administración
 * de usuarios. Muchos de estos requieren credenciales de moderador
 * o administrador, por lo que se acepta cualquier código de estado
 * inferior a 500 como respuesta válida. Puedes ajustar los IDs
 * utilizados para que coincidan con los existentes en tu base de datos.
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
  // Usuarios por rol
  const resByRole = http.get(`${BASE_URL}/admin/users/by-role?roleName=USER`);
  check(resByRole, {
    'GET /admin/users/by-role status < 500': (r) => r.status < 500,
  });

  // Usuarios por estado
  const resByState = http.get(`${BASE_URL}/admin/users/by-state?stateName=ACTIVE`);
  check(resByState, {
    'GET /admin/users/by-state status < 500': (r) => r.status < 500,
  });

  // Historial de un usuario concreto (ID 1 por defecto)
  const resHistory = http.get(`${BASE_URL}/admin/users/1/history`);
  check(resHistory, {
    'GET /admin/users/1/history status < 500': (r) => r.status < 500,
  });

  sleep(1);
}
