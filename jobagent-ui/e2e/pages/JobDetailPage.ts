import type { Page } from '@playwright/test'

export class JobDetailPage {
  constructor(private page: Page) {}

  get title() {
    return this.page.locator('h1')
  }

  get company() {
    return this.page.locator('.text-lg.text-gray-600')
  }

  get location() {
    return this.page.locator('text=📍')
  }

  get description() {
    return this.page.getByText('Description').locator('..')
  }

  get applyButton() {
    return this.page.getByRole('link', { name: /apply now/i })
  }

  get backLink() {
    return this.page.getByRole('link', { name: /back to search/i })
  }

  get matchScore() {
    return this.page.locator('text=/\\d+% match/')
  }
}
