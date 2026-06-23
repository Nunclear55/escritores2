import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para FollowController.
 *
 * Realiza llamadas GET para obtener el número de seguidores y el
 * listado de seguidores de un usuario concreto (ID 1 por defecto).
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
  const resCount = http.get(`${BASE_URL}/follows/user/1/count`);
  check(resCount, {
    'GET /follows/user/1/count status < 500': (r) => r.status < 500,
  });

  const resFollowers = http.get(`${BASE_URL}/follows/user/1/followers`);
  check(resFollowers, {
    'GET /follows/user/1/followers status < 500': (r) => r.status < 500,
  });

  sleep(1);
}
