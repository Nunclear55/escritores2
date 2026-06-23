import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para ItemController.
 *
 * Se generan solicitudes GET para obtener ítems por historia y un ítem
 * individual.  Acepta cualquier estado de respuesta inferior a 500.
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
  const resItemsByStory = http.get(`${BASE_URL}/items/story/1`);
  check(resItemsByStory, {
    'GET /items/story/1 status < 500': (r) => r.status < 500,
  });

  const resItem = http.get(`${BASE_URL}/items/1`);
  check(resItem, {
    'GET /items/1 status < 500': (r) => r.status < 500,
  });

  sleep(1);
}
