import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para VolumeController.
 *
 * Este script envía peticiones GET a los endpoints de volúmenes para
 * obtener un volumen por ID y listar los volúmenes de una historia.
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
  const resVolume = http.get(`${BASE_URL}/volumes/1`);
  check(resVolume, {
    'GET /volumes/1 status < 500': (r) => r.status < 500,
  });

  const resVolumesByStory = http.get(`${BASE_URL}/volumes/story/1`);
  check(resVolumesByStory, {
    'GET /volumes/story/1 status < 500': (r) => r.status < 500,
  });

  sleep(1);
}
