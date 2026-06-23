import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para CommentController.
 *
 * Incluye solicitudes para obtener comentarios de una historia,
 * obtener un comentario individual y listar las respuestas de un
 * comentario. Estos endpoints son públicos, pero el resultado puede
 * variar según la existencia del recurso; se acepta cualquier
 * estado < 500.
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
  const resCommentsByStory = http.get(`${BASE_URL}/comments/story/1`);
  check(resCommentsByStory, {
    'GET /comments/story/1 status < 500': (r) => r.status < 500,
  });

  const resComment = http.get(`${BASE_URL}/comments/1`);
  check(resComment, {
    'GET /comments/1 status < 500': (r) => r.status < 500,
  });

  const resReplies = http.get(`${BASE_URL}/comments/1/replies`);
  check(resReplies, {
    'GET /comments/1/replies status < 500': (r) => r.status < 500,
  });

  sleep(1);
}
