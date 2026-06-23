import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para CharacterController.
 *
 * Realiza peticiones a los endpoints de listado y detalle de
 * personajes. Incluye una búsqueda simple usando query para
 * evaluar el rendimiento de filtros.
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
  // Personajes de una historia
  const resChars = http.get(`${BASE_URL}/characters/story/1`);
  check(resChars, {
    'GET /characters/story/1 status < 500': (r) => r.status < 500,
  });

  // Personaje individual
  const resChar = http.get(`${BASE_URL}/characters/1`);
  check(resChar, {
    'GET /characters/1 status < 500': (r) => r.status < 500,
  });

  // Búsqueda de personajes
  const resSearch = http.get(`${BASE_URL}/characters/search?q=hero`);
  check(resSearch, {
    'GET /characters/search status < 500': (r) => r.status < 500,
  });

  sleep(1);
}
