import type { Page } from '@playwright/test'

export class LoginPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/login')
  }

  async login(email: string, password: string) {
    await this.page.getByLabel('Email').fill(email)
    await this.page.getByLabel('Password').fill(password)
    await this.page.getByRole('button', { name: /sign in/i }).click()
  }

  async getErrorMessage() {
    return this.page.locator('.bg-red-50').textContent()
  }

  get heading() {
    return this.page.getByRole('heading', { name: /login/i })
  }

  get registerLink() {
    return this.page.getByRole('link', { name: /register/i })
  }
}
