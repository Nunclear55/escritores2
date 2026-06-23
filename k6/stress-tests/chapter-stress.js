import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para ChapterController.
 *
 * Envía solicitudes GET para obtener capítulos individuales y
 * listas de capítulos de una historia.  Acepta respuestas con
 * estados < 500 para contemplar tanto éxitos como errores de
 * autorización o recursos inexistentes.
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
  const resChapter = http.get(`${BASE_URL}/chapters/1`);
  check(resChapter, {
    'GET /chapters/1 status < 500': (r) => r.status < 500,
  });

  const resChaptersByStory = http.get(`${BASE_URL}/chapters/story/1`);
  check(resChaptersByStory, {
    'GET /chapters/story/1 status < 500': (r) => r.status < 500,
  });

  sleep(1);
}
