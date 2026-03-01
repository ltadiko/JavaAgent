import { test, expect } from '@playwright/test'
import { loginViaUI, registerViaUI } from '../fixtures/auth.fixture'
import { TEST_USER, TIMEOUTS } from '../fixtures/test-data'
import { LoginPage } from '../pages/LoginPage'
import { RegisterPage } from '../pages/RegisterPage'

test.describe('Authentication Flow', () => {

  test('register new user → redirects to dashboard', async ({ page }) => {
    await registerViaUI(
      page,
      TEST_USER.fullName,
      TEST_USER.email,
      TEST_USER.password,
      TEST_USER.region,
    )
    await expect(page).toHaveURL(/\/dashboard/)
  })

  test('login with valid credentials → redirects to dashboard', async ({ page }) => {
    // Login with the user just registered
    await loginViaUI(page, TEST_USER.email, TEST_USER.password)
    await expect(page).toHaveURL(/\/dashboard/)
  })

  test('login with wrong password → shows error', async ({ page }) => {
    const loginPage = new LoginPage(page)
    await loginPage.goto()
    await loginPage.login(TEST_USER.email, 'WrongPassword123!')

    // Should stay on login page and show error
    await expect(page).toHaveURL(/\/login/)
    await expect(page.locator('.bg-red-50')).toBeVisible({ timeout: TIMEOUTS.apiResponse })
  })

  test('register page has link to login', async ({ page }) => {
    const registerPage = new RegisterPage(page)
    await registerPage.goto()
    await expect(registerPage.heading).toBeVisible()
    await expect(registerPage.loginLink).toBeVisible()
  })

  test('login page has link to register', async ({ page }) => {
    const loginPage = new LoginPage(page)
    await loginPage.goto()
    await expect(loginPage.heading).toBeVisible()
    await expect(loginPage.registerLink).toBeVisible()
  })

  test('unauthenticated user → redirected to login', async ({ page }) => {
    // Clear any stored tokens
    await page.goto('/login')
    await page.evaluate(() => localStorage.clear())

    await page.goto('/dashboard')
    await expect(page).toHaveURL(/\/login/)
  })

  test('logout clears session and redirects to login', async ({ page }) => {
    // First login
    await loginViaUI(page, TEST_USER.email, TEST_USER.password)
    await expect(page).toHaveURL(/\/dashboard/)

    // Logout
    await page.getByRole('button', { name: /logout/i }).click()
    await expect(page).toHaveURL(/\/login/)

    // Verify can't access dashboard
    await page.goto('/dashboard')
    await expect(page).toHaveURL(/\/login/)
  })
})
