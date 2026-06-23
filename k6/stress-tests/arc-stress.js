import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para los endpoints del controlador ArcController.
 *
 * Este script realiza peticiones GET a los endpoints que devuelven
 * arcos por ID y por historia.  Se acepta cualquier código de
 * estado inferior a 500 para permitir tanto respuestas exitosas
 * como errores de autorización o inexistencia del recurso.
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
  // Obtener arco por ID
  const resArc = http.get(`${BASE_URL}/arcs/1`);
  check(resArc, {
    'GET /arcs/1 status < 500': (r) => r.status < 500,
  });

  // Obtener arcos de una historia
  const resArcsByStory = http.get(`${BASE_URL}/arcs/story/1`);
  check(resArcsByStory, {
    'GET /arcs/story/1 status < 500': (r) => r.status < 500,
  });

  sleep(1);
}
