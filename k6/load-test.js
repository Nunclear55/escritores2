import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 10,          // 10 usuarios virtuales
    duration: '30s', // duración de la prueba
};

const BASE_URL = 'http://localhost:8080';

export default function () {

    const response = http.get(`${BASE_URL}/stories`);

    check(response, {
        'status 200': (r) => r.status === 200,
    });

    sleep(1);
}