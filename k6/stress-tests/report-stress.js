import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para ReportController.
 *
 * Se someten a carga los principales endpoints de consulta de reportes.
 * Dado que la mayoría requieren permisos de moderador o administrador,
 * se acepta cualquier código de estado inferior a 500 para reflejar
 * tanto respuestas exitosas como errores de autorización o recursos
 * inexistentes.
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
  // Reportes pendientes
  const resPending = http.get(`${BASE_URL}/reports/pending`);
  check(resPending, {
    'GET /reports/pending status < 500': (r) => r.status < 500,
  });

  // Reportes por estado
  const resStatus = http.get(`${BASE_URL}/reports?statusName=PENDING`);
  check(resStatus, {
    'GET /reports?statusName=PENDING status < 500': (r) => r.status < 500,
  });

  // Reporte individual
  const resReport = http.get(`${BASE_URL}/reports/1`);
  check(resReport, {
    'GET /reports/1 status < 500': (r) => r.status < 500,
  });

  // Historial de reportes
  const resHistory = http.get(`${BASE_URL}/reports/history`);
  check(resHistory, {
    'GET /reports/history status < 500': (r) => r.status < 500,
  });

  sleep(1);
}
