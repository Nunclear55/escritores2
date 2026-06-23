import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para MediaController.
 *
 * Realiza solicitudes a los endpoints de descarga y consulta de medios.
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
  const resByChapter = http.get(`${BASE_URL}/media/chapter/1`);
  check(resByChapter, {
    'GET /media/chapter/1 status < 500': (r) => r.status < 500,
  });

  const resMedia = http.get(`${BASE_URL}/media/1`);
  check(resMedia, {
    'GET /media/1 status < 500': (r) => r.status < 500,
  });

  const resDownload = http.get(`${BASE_URL}/media/1/download`);
  check(resDownload, {
    'GET /media/1/download status < 500': (r) => r.status < 500,
  });

  sleep(1);
}
