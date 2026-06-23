import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para MetricsController.
 *
 * Este script consulta las historias más vistas para ejercitar los
 * cálculos de métricas y carga la base de datos. Puede devolver 200
 * si hay datos o 404 si la lista está vacía; cualquier estado < 500
 * es aceptable.
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
  const resTopStories = http.get(`${BASE_URL}/metrics/stories/top-viewed`);
  check(resTopStories, {
    'GET /metrics/stories/top-viewed status < 500': (r) => r.status < 500,
  });
  sleep(1);
}
