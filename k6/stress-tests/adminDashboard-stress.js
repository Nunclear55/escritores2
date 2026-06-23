import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para los endpoints del controlador AdminDashboardController.
 *
 * Este script utiliza k6 para simular múltiples usuarios concurrentes
 * realizando peticiones GET contra los endpoints de administración del
 * tablero.  La configuración de cargas escalonadas permite observar
 * cómo responde la API ante distintos volúmenes de tráfico.  Ajusta
 * las etapas según tus necesidades de prueba.
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
  // Endpoint resumen de dashboard administrativo
  const resSummary = http.get(`${BASE_URL}/admin/dashboard/summary`);
  check(resSummary, {
    'GET /admin/dashboard/summary status < 500': (r) => r.status < 500,
  });

  // Endpoint de actividad del dashboard administrativo
  const resActivity = http.get(`${BASE_URL}/admin/dashboard/activity`);
  check(resActivity, {
    'GET /admin/dashboard/activity status < 500': (r) => r.status < 500,
  });

  sleep(1);
}
