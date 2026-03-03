import { test, expect } from '@playwright/test'
import { loginViaUI, registerViaUI, registerViaAPI, loginViaAPI } from '../fixtures/auth.fixture'
import { TEST_USER, TIMEOUTS } from '../fixtures/test-data'
import { LoginPage } from '../pages/LoginPage'
import { RegisterPage } from '../pages/RegisterPage'

test.describe('Authentication Flow', () => {

  test('register new user → redirects to dashboard', async ({ page, baseURL }) => {
    const ts = Date.now()
    const email = `e2e-reg-${ts}@jobagent.test`

    await registerViaUI(page, `E2E User ${ts}`, email, 'TestPass123!', 'DE')
    await expect(page).toHaveURL(/\/dashboard/)
  })

  test('login with valid credentials → redirects to dashboard', async ({ page, baseURL }) => {
    const proxyBase = baseURL || 'http://127.0.0.1:5173'
    const ts = Date.now()
    const email = `e2e-login-${ts}@jobagent.test`
    const password = 'TestPass123!'

    // Pre-register user via API
    await registerViaAPI(proxyBase, 'Login Test User', email, password, 'DE')

    // Now login via UI
    await loginViaUI(page, email, password)
    await expect(page).toHaveURL(/\/dashboard/)
  })

  test('login with wrong password → shows error', async ({ page, baseURL }) => {
    const proxyBase = baseURL || 'http://127.0.0.1:5173'
    const ts = Date.now()
    const email = `e2e-wrong-${ts}@jobagent.test`

    // Pre-register user
    await registerViaAPI(proxyBase, 'Wrong Pwd User', email, 'TestPass123!', 'DE')

    const loginPage = new LoginPage(page)
    await loginPage.goto()
    await loginPage.login(email, 'WrongPassword123!')

    // Should stay on login page
    await expect(page).toHaveURL(/\/login/, { timeout: TIMEOUTS.apiResponse })

    // Should show error message (bg-red-50 or text-red-600)
    const errorEl = page.locator('.bg-red-50, .text-red-600')
    await expect(errorEl.first()).toBeVisible({ timeout: TIMEOUTS.apiResponse })
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

  test('logout clears session and redirects to login', async ({ page, baseURL }) => {
    const proxyBase = baseURL || 'http://127.0.0.1:5173'
    const ts = Date.now()
    const email = `e2e-logout-${ts}@jobagent.test`
    const password = 'TestPass123!'

    // Register user via API and get token
    await registerViaAPI(proxyBase, 'Logout Test User', email, password, 'DE')
    const token = await loginViaAPI(proxyBase, email, password)

    // Inject token and navigate to dashboard
    await page.goto('/login')
    await page.evaluate((t) => localStorage.setItem('access_token', t), token)
    await page.goto('/dashboard')
    await expect(page).toHaveURL(/\/dashboard/)

    // Logout
    await page.getByRole('button', { name: /logout/i }).click()
    await expect(page).toHaveURL(/\/login/)

    // Verify can't access dashboard
    await page.goto('/dashboard')
    await expect(page).toHaveURL(/\/login/)
  })
})
