import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para SkillController.
 *
 * Este script consulta habilidades por ID, por historia y realiza una
 * búsqueda general.  Se acepta cualquier estado de respuesta
 * inferior a 500 para contemplar tanto respuestas exitosas como
 * fallidas.
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
  const resSkill = http.get(`${BASE_URL}/skills/1`);
  check(resSkill, {
    'GET /skills/1 status < 500': (r) => r.status < 500,
  });

  const resSkillsByStory = http.get(`${BASE_URL}/skills/story/1`);
  check(resSkillsByStory, {
    'GET /skills/story/1 status < 500': (r) => r.status < 500,
  });

  const resSearch = http.get(`${BASE_URL}/skills/search?q=magic`);
  check(resSearch, {
    'GET /skills/search status < 500': (r) => r.status < 500,
  });

  sleep(1);
}
