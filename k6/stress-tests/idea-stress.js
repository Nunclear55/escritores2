import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para IdeaController.
 *
 * Incluye solicitudes para obtener ideas por historia y una idea individual.
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
  const resIdeasByStory = http.get(`${BASE_URL}/ideas/story/1`);
  check(resIdeasByStory, {
    'GET /ideas/story/1 status < 500': (r) => r.status < 500,
  });

  const resIdea = http.get(`${BASE_URL}/ideas/1`);
  check(resIdea, {
    'GET /ideas/1 status < 500': (r) => r.status < 500,
  });

  sleep(1);
}
