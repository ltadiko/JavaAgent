/**
 * Test data constants for E2E tests.
 * Uses a unique timestamp suffix to avoid collisions between test runs.
 */
const timestamp = Date.now()

export const TEST_USER = {
  fullName: `E2E Test User ${timestamp}`,
  email: `e2e-test-${timestamp}@jobagent.test`,
  password: 'TestPass123!',
  region: 'EU',
}

export const EXISTING_USER = {
  email: 'e2e-user@jobagent.test',
  password: 'TestPass123!',
  fullName: 'E2E Permanent User',
  region: 'EU',
}

export const JOB_SEARCH = {
  query: 'Software Engineer',
  location: 'Berlin',
}

export const TIMEOUTS = {
  navigation: 10_000,
  apiResponse: 15_000,
  fileUpload: 20_000,
}
