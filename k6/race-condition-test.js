import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 10,
  duration: '10s',
};

const BASE_URL = 'http://localhost:8080';

function login() {
  const res = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
    username: 'lucas',
    tenantId: 'tenant-001',
    role: 'BUYER'
  }), { headers: { 'Content-Type': 'application/json' } });

  return JSON.parse(res.body).accessToken;
}

export default function () {
  const token = login();

  const payload = JSON.stringify({
    buyerId: '11111111-1111-1111-1111-111111111111',
    supplierId: '22222222-2222-2222-2222-222222222222',
    idempotencyKey: `order-race-${__VU}-${__ITER}`,
    items: [
      {
        productName: 'Notebook',
        quantity: 1,
        unitPrice: 1000.00,
        currency: 'BRL'
      }
    ]
  });

  const res = http.post(`${BASE_URL}/orders`, payload, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  });

  check(res, {
    'status is 201': (r) => r.status === 201,
  });

  sleep(0.1);
}