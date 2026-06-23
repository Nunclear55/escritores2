import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para DashboardController.
 *
 * El endpoint `/dashboard/me/summary` está protegido y requiere
 * autenticación; aceptamos cualquier estado 200, 401 o 403.
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
  const resDashboard = http.get(`${BASE_URL}/dashboard/me/summary`);
  check(resDashboard, {
    'GET /dashboard/me/summary status < 500': (r) => r.status < 500,
  });
  sleep(1);
}
