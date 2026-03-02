import type { Page } from '@playwright/test'

export class RegisterPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/register')
  }

  async register(fullName: string, email: string, password: string, country = 'DE') {
    await this.page.getByLabel('Full Name').fill(fullName)
    await this.page.getByLabel('Email').fill(email)
    await this.page.getByLabel('Password').fill(password)
    await this.page.getByLabel('Country').selectOption(country)
    await this.page.getByRole('button', { name: /register/i }).click()
  }

  async getErrorMessage() {
    return this.page.locator('.bg-red-50').textContent()
  }

  get heading() {
    return this.page.getByRole('heading', { name: /create account/i })
  }

  get loginLink() {
    return this.page.getByRole('link', { name: /sign in/i })
  }
}
