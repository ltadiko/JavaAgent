import { test, expect } from '@playwright/test'
import { TEST_USER } from '../fixtures/test-data'

test.describe('Debug Auth Flow', () => {

  test('debug register via API directly', async ({ page, request }) => {
    const ts = Date.now()
    const email = `debug-${ts}@jobagent.test`

    // Test register API directly
    const regRes = await request.post('http://127.0.0.1:8080/api/v1/auth/register', {
      data: { fullName: 'Debug User', email, password: 'TestPass123!', country: 'DE' }
    })
    expect(regRes.status()).toBe(201)
    const regBody = await regRes.json()
    console.log('[REGISTER API]', JSON.stringify(regBody))

    // Test login API directly
    const loginRes = await request.post('http://127.0.0.1:8080/api/v1/auth/login', {
      data: { email, password: 'TestPass123!' }
    })
    expect(loginRes.status()).toBe(200)
    const loginBody = await loginRes.json()
    console.log('[LOGIN API] token_type=', loginBody.token_type, 'has_access_token=', !!loginBody.access_token)

    // Test /me API with token
    const meRes = await request.get('http://127.0.0.1:8080/api/v1/auth/me', {
      headers: { Authorization: `Bearer ${loginBody.access_token}` }
    })
    expect(meRes.status()).toBe(200)
    const meBody = await meRes.json()
    console.log('[ME API]', JSON.stringify(meBody))
  })

  test('debug register via UI flow', async ({ page }) => {
    const apiResponses: string[] = []

    // Intercept ALL responses
    page.on('response', async (response) => {
      const url = response.url()
      if (url.includes('/api/')) {
        let body = ''
        try { body = await response.text() } catch {}
        apiResponses.push(`${response.status()} ${response.request().method()} ${url} → ${body.substring(0, 200)}`)
      }
    })

    page.on('console', msg => {
      if (msg.type() === 'error') {
        apiResponses.push(`[CONSOLE ERROR] ${msg.text()}`)
      }
    })

    page.on('requestfailed', req => {
      apiResponses.push(`[REQUEST FAILED] ${req.method()} ${req.url()} ${req.failure()?.errorText}`)
    })

    await page.goto('/register')
    await page.waitForTimeout(500)

    const ts = Date.now()
    await page.getByLabel('Full Name').fill(`UI Test ${ts}`)
    await page.getByLabel('Email').fill(`ui-test-${ts}@jobagent.test`)
    await page.getByLabel('Password').fill('TestPass123!')
    await page.getByLabel('Country').selectOption('DE')

    await page.getByRole('button', { name: /register/i }).click()

    // Wait for API calls
    await page.waitForTimeout(8000)

    const url = page.url()
    const errorEl = page.locator('.bg-red-50')
    const hasError = await errorEl.isVisible()
    const errorText = hasError ? await errorEl.textContent() : 'none'

    // Print all captured info
    for (const r of apiResponses) {
      console.log('[API]', r)
    }
    console.log('[FINAL URL]', url)
    console.log('[ERROR VISIBLE]', hasError, errorText)

    // The test should end up on dashboard
    expect(url).toContain('/dashboard')
  })
})
