import { test as base, type Page } from '@playwright/test'
import { EXISTING_USER, TIMEOUTS } from './test-data'

/**
 * Auth fixture — provides an `authenticatedPage` that is already logged in.
 */
export const test = base.extend<{ authenticatedPage: Page }>({
  authenticatedPage: async ({ page }, use) => {
    await loginViaUI(page, EXISTING_USER.email, EXISTING_USER.password)
    await use(page)
  },
})

/**
 * Login via the UI form.
 */
export async function loginViaUI(page: Page, email: string, password: string) {
  await page.goto('/login')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill(password)
  await page.getByRole('button', { name: /sign in/i }).click()
  await page.waitForURL('**/dashboard', { timeout: TIMEOUTS.navigation })
}

/**
 * Register a new user via the UI form.
 */
export async function registerViaUI(
  page: Page,
  fullName: string,
  email: string,
  password: string,
  region = 'EU'
) {
  await page.goto('/register')
  await page.getByLabel('Full Name').fill(fullName)
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill(password)
  await page.getByLabel('Region').selectOption(region)
  await page.getByRole('button', { name: /register/i }).click()
  await page.waitForURL('**/dashboard', { timeout: TIMEOUTS.navigation })
}

export { expect } from '@playwright/test'
