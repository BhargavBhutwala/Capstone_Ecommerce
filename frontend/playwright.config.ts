import { defineConfig, devices } from '@playwright/test'

/**
 * Playwright configuration for E2E tests against real backend + Vite frontend.
 *
 * Backend:  http://localhost:8080/api  (Spring Boot, SPRING_PROFILES_ACTIVE=local)
 * Frontend: http://localhost:5173      (Vite dev server)
 *
 * Start both servers externally before running: npm run integration
 */
export default defineConfig({
  testDir: './src/test/e2e',
  timeout: 60_000,
  retries: 0,
  reporter: 'list',
  use: {
    baseURL: 'http://localhost:5173',
    headless: true,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
})
