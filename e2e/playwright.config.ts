import { defineConfig, devices } from '@playwright/test'

/**
 * Playwright E2E configuration for JobAgent.
 *
 * This is a standalone E2E module — decoupled from the UI (jobagent-ui)
 * and backend (Spring Boot) modules. It tests the full stack end-to-end.
 *
 * Prerequisites:
 *   1. docker compose up -d          (PostgreSQL, MinIO, etc.)
 *   2. Backend on :8080              (mvn spring-boot:run -Dspring-boot.run.profiles=local)
 *   3. Vite dev server on :5173      (auto-started by webServer config below)
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: false,            // Run sequentially — flows depend on auth state
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,                      // Single worker for sequential E2E flows
  reporter: [
    ['html', { open: 'never' }],
    ['list'],
  ],
  timeout: 30_000,
  expect: { timeout: 10_000 },

  use: {
    baseURL: 'http://127.0.0.1:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  /* Auto-start the Vite dev server from the UI module */
  webServer: {
    command: 'npm run dev -- --host 127.0.0.1 --port 5173',
    cwd: '../jobagent-ui',
    url: 'http://127.0.0.1:5173',
    reuseExistingServer: true,
    timeout: 30_000,
  },
})
