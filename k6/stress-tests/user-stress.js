import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para UserController.
 *
 * Consulta varios endpoints de usuarios, tanto públicos como
 * privados. Se incluyen llamados al perfil de un usuario, al perfil
 * propio, al listado de usuarios, a la búsqueda de usuarios y a los
 * endpoints de perfil público y listados de historias de un autor. Al
 * requerir autenticación para algunas rutas, se admite cualquier
 * código de estado inferior a 500.
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
  // Perfil de un usuario por ID
  const resUser = http.get(`${BASE_URL}/users/1`);
  check(resUser, {
    'GET /users/1 status < 500': (r) => r.status < 500,
  });

  // Perfil del usuario autenticado
  const resMe = http.get(`${BASE_URL}/users/me`);
  check(resMe, {
    'GET /users/me status < 500': (r) => r.status < 500,
  });

  // Listado de usuarios (administración)
  const resList = http.get(`${BASE_URL}/users`);
  check(resList, {
    'GET /users status < 500': (r) => r.status < 500,
  });

  // Búsqueda de usuarios
  const resSearch = http.get(`${BASE_URL}/users/search?q=john`);
  check(resSearch, {
    'GET /users/search status < 500': (r) => r.status < 500,
  });

  // Perfil público de un autor
  const resPublic = http.get(`${BASE_URL}/users/1/public-profile`);
  check(resPublic, {
    'GET /users/1/public-profile status < 500': (r) => r.status < 500,
  });

  // Historias públicas de un autor
  const resStories = http.get(`${BASE_URL}/users/1/stories`);
  check(resStories, {
    'GET /users/1/stories status < 500': (r) => r.status < 500,
  });

  sleep(1);
}
