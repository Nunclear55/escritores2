import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para GlobalNoticeController.
 *
 * Envía solicitudes GET para obtener avisos activos y un aviso individual.
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
  const resActive = http.get(`${BASE_URL}/global-notices/active`);
  check(resActive, {
    'GET /global-notices/active status < 500': (r) => r.status < 500,
  });

  const resNotice = http.get(`${BASE_URL}/global-notices/1`);
  check(resNotice, {
    'GET /global-notices/1 status < 500': (r) => r.status < 500,
  });

  sleep(1);
}
