import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para CommentModerationController.
 *
 * Este controlador expone un único endpoint público para listar
 * comentarios ocultos para moderadores. El resultado puede ser
 * 200, 401 o 403 dependiendo de la autenticación; se acepta
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
  const resHidden = http.get(`${BASE_URL}/moderation/comments/hidden`);
  check(resHidden, {
    'GET /moderation/comments/hidden status < 500': (r) => r.status < 500,
  });
  sleep(1);
}
