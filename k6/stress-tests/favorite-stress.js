import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para FavoriteController.
 *
 * Solo se expone un endpoint público para contar favoritos de una historia.
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
  const resFavoriteCount = http.get(`${BASE_URL}/favorites/story/1/count`);
  check(resFavoriteCount, {
    'GET /favorites/story/1/count status < 500': (r) => r.status < 500,
  });
  sleep(1);
}
