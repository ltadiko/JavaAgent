import { defineConfig, devices } from '@playwright/test'

/**
 * Playwright E2E configuration for JobAgent UI.
 *
 * Prerequisites:
 *   1. docker compose up -d  (PostgreSQL, Valkey, Kafka, MinIO, Ollama)
 *   2. Backend running on :8080  (mvn spring-boot:run -Dspring-boot.run.profiles=local)
 *   3. Vite dev server on :5173  (npm run dev)  — OR use webServer below
 */
export default defineConfig({
  testDir: './e2e/tests',
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
    baseURL: 'http://localhost:5173',
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

  /* Optionally start Vite dev server automatically */
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: true,      // Don't start if already running
    timeout: 30_000,
  },
})
