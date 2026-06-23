import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para StoryController.
 *
 * Ejecuta una serie de solicitudes a los endpoints públicos de
 * historias: listado, búsqueda, obtención por ID, obtención por
 * slug, historias por usuario y los listados de borradores y
 * archivados del usuario autenticado.  Se aceptan respuestas con
 * estados < 500 para contemplar tanto resultados exitosos como
 * respuestas de error (401/403/404).
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
  // Listar historias públicas
  const resList = http.get(`${BASE_URL}/stories`);
  check(resList, {
    'GET /stories status < 500': (r) => r.status < 500,
  });

  // Buscar historias
  const resSearch = http.get(`${BASE_URL}/stories/search?q=adventure`);
  check(resSearch, {
    'GET /stories/search status < 500': (r) => r.status < 500,
  });

  // Historia por ID
  const resStory = http.get(`${BASE_URL}/stories/1`);
  check(resStory, {
    'GET /stories/1 status < 500': (r) => r.status < 500,
  });

  // Historia por slug (usa un slug genérico; podría devolver 404)
  const resSlug = http.get(`${BASE_URL}/stories/slug/test-slug`);
  check(resSlug, {
    'GET /stories/slug/test-slug status < 500': (r) => r.status < 500,
  });

  // Historias por usuario
  const resByUser = http.get(`${BASE_URL}/stories/user/1`);
  check(resByUser, {
    'GET /stories/user/1 status < 500': (r) => r.status < 500,
  });

  // Borradores del usuario autenticado
  const resDrafts = http.get(`${BASE_URL}/stories/me/drafts`);
  check(resDrafts, {
    'GET /stories/me/drafts status < 500': (r) => r.status < 500,
  });

  // Historias archivadas del usuario autenticado
  const resArchived = http.get(`${BASE_URL}/stories/me/archived`);
  check(resArchived, {
    'GET /stories/me/archived status < 500': (r) => r.status < 500,
  });

  sleep(1);
}
