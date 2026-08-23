/**
 * E2E helpers shared across tests.
 *
 * Uses a deterministic test user seed for the test suite.
 * The backend must be running on http://localhost:8080/api.
 * The Vite frontend must be running on http://localhost:5173.
 */

export const TEST_USER = {
  firstName: 'E2E',
  lastName: 'Tester',
  email: `e2e-${Date.now()}@example.com`,
  password: 'E2ETest!2024',
}

export const BASE_URL = 'http://localhost:5173'
export const API_URL = 'http://localhost:8080/api'
