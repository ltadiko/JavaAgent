import { test as base, type Page } from '@playwright/test'
import { TIMEOUTS } from './test-data'

/**
 * Seed user via direct API call (bypasses UI for speed & reliability).
 * Returns access_token so the page can be pre-authenticated.
 */
async function ensureTestUser(baseURL: string) {
  const ts = Date.now()
  const email = `fixture-${ts}@jobagent.test`
  const password = 'TestPass123!'

  // Register via API
  await fetch(`${baseURL}/api/v1/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fullName: 'Fixture User', email, password, country: 'DE' }),
  })

  // Login via API
  const loginRes = await fetch(`${baseURL}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  const { access_token } = await loginRes.json()
  return { email, password, access_token }
}

/**
 * Auth fixture — provides an `authenticatedPage` that is already logged in
 * by injecting the JWT token into localStorage (faster, more reliable than UI login).
 */
export const test = base.extend<{ authenticatedPage: Page }>({
  authenticatedPage: async ({ page, baseURL }, use) => {
    const proxyBase = baseURL || 'http://127.0.0.1:5173'
    const { access_token } = await ensureTestUser(proxyBase)

    // Navigate to login first to set the origin
    await page.goto('/login')
    // Inject token into localStorage
    await page.evaluate((token) => {
      localStorage.setItem('access_token', token)
    }, access_token)
    // Now navigate to dashboard — auth guard will allow
    await page.goto('/dashboard')
    await page.waitForURL('**/dashboard', { timeout: TIMEOUTS.navigation })
    await use(page)
  },
})

/**
 * Login via the UI form.
 * Waits for the /api/v1/auth/login response before checking redirect.
 */
export async function loginViaUI(page: Page, email: string, password: string) {
  await page.goto('/login')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill(password)

  // Click and wait for the login API response
  const [loginResponse] = await Promise.all([
    page.waitForResponse(resp => resp.url().includes('/api/v1/auth/login'), { timeout: TIMEOUTS.apiResponse }),
    page.getByRole('button', { name: /sign in/i }).click(),
  ])

  if (loginResponse.status() === 200) {
    await page.waitForURL('**/dashboard', { timeout: TIMEOUTS.navigation })
  }
  // If login failed, caller checks the page state
}

/**
 * Register a new user via the UI form.
 * Waits for register API response, then login API response, then dashboard redirect.
 */
export async function registerViaUI(
  page: Page,
  fullName: string,
  email: string,
  password: string,
  country = 'DE'
) {
  await page.goto('/register')
  await page.getByLabel('Full Name').fill(fullName)
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill(password)
  await page.getByLabel('Country').selectOption(country)

  // Click register — expect register API call, then auto-login API call
  const [registerResponse] = await Promise.all([
    page.waitForResponse(resp => resp.url().includes('/api/v1/auth/register'), { timeout: TIMEOUTS.apiResponse }),
    page.getByRole('button', { name: /register/i }).click(),
  ])

  if (registerResponse.status() === 201) {
    // Wait for the auto-login that the store triggers after register
    await page.waitForResponse(resp => resp.url().includes('/api/v1/auth/login'), { timeout: TIMEOUTS.apiResponse })
    await page.waitForURL('**/dashboard', { timeout: TIMEOUTS.navigation })
  }
  // If register failed (e.g. duplicate), caller checks page state
}

/**
 * Register a user via direct API call (faster, for setup purposes).
 */
export async function registerViaAPI(baseURL: string, fullName: string, email: string, password: string, country = 'DE') {
  const res = await fetch(`${baseURL}/api/v1/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fullName, email, password, country }),
  })
  return res
}

/**
 * Login via direct API call and return token.
 */
export async function loginViaAPI(baseURL: string, email: string, password: string) {
  const res = await fetch(`${baseURL}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  const body = await res.json()
  return body.access_token as string
}

export { expect } from '@playwright/test'
