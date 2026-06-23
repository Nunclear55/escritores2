import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Prueba de estrés para CharacterSkillController.
 *
 * Accede a la lista de habilidades por personaje y por habilidad.  Los
 * endpoints requieren roles según la configuración de seguridad; por
 * tanto, cualquier código < 500 se considera aceptable.
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
  const resByCharacter = http.get(`${BASE_URL}/character-skills/character/1`);
  check(resByCharacter, {
    'GET /character-skills/character/1 status < 500': (r) => r.status < 500,
  });

  const resBySkill = http.get(`${BASE_URL}/character-skills/skill/1`);
  check(resBySkill, {
    'GET /character-skills/skill/1 status < 500': (r) => r.status < 500,
  });

  sleep(1);
}
