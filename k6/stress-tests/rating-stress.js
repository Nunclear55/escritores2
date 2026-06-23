import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para RatingController.
 *
 * Envía peticiones GET para obtener valoraciones de una historia,
 * calcular el promedio de valoraciones y consultar una valoración
 * individual. Acepta cualquier estado de respuesta inferior a 500.
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
  const resRatings = http.get(`${BASE_URL}/ratings/story/1`);
  check(resRatings, {
    'GET /ratings/story/1 status < 500': (r) => r.status < 500,
  });

  const resAvg = http.get(`${BASE_URL}/ratings/story/1/average`);
  check(resAvg, {
    'GET /ratings/story/1/average status < 500': (r) => r.status < 500,
  });

  const resRating = http.get(`${BASE_URL}/ratings/1`);
  check(resRating, {
    'GET /ratings/1 status < 500': (r) => r.status < 500,
  });

  sleep(1);
}
