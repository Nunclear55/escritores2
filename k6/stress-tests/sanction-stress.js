import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para SanctionController.
 *
 * Se realizan peticiones GET a los endpoints de sanciones para un
 * usuario concreto, las sanciones del usuario autenticado y las
 * sanciones activas. Estos endpoints requieren roles de moderador o
 * superior, por lo que se acepta cualquier estado < 500.
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
  const resByUser = http.get(`${BASE_URL}/sanctions/user/1`);
  check(resByUser, {
    'GET /sanctions/user/1 status < 500': (r) => r.status < 500,
  });

  const resMe = http.get(`${BASE_URL}/sanctions/me`);
  check(resMe, {
    'GET /sanctions/me status < 500': (r) => r.status < 500,
  });

  const resActive = http.get(`${BASE_URL}/sanctions/active`);
  check(resActive, {
    'GET /sanctions/active status < 500': (r) => r.status < 500,
  });

  sleep(1);
}
