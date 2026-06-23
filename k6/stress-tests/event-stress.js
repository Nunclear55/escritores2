import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para EventController.
 *
 * Se realizan solicitudes GET a los eventos asociados a una historia y
 * a un evento individual. Los resultados pueden variar; se acepta
 * cualquier estado inferior a 500.
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
  const resEventsByStory = http.get(`${BASE_URL}/events/story/1`);
  check(resEventsByStory, {
    'GET /events/story/1 status < 500': (r) => r.status < 500,
  });

  const resEvent = http.get(`${BASE_URL}/events/1`);
  check(resEvent, {
    'GET /events/1 status < 500': (r) => r.status < 500,
  });

  sleep(1);
}
