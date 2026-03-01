import type { Page, Locator } from '@playwright/test'

export class JobSearchPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/jobs')
  }

  get heading() {
    return this.page.getByRole('heading', { name: /job search/i })
  }

  async search(query: string, location?: string) {
    await this.page.locator('input[placeholder*="Job title"]').fill(query)
    if (location) {
      await this.page.locator('input[placeholder*="Location"]').fill(location)
    }
    await this.page.getByRole('button', { name: /search/i }).click()
  }

  get resultCount() {
    return this.page.locator('text=/\\d+ jobs found/')
  }

  get jobCards(): Locator {
    return this.page.locator('.rounded-lg.bg-white.p-4.shadow')
  }

  get loadingIndicator() {
    return this.page.getByText('Searching...')
  }

  get emptyState() {
    return this.page.getByText('No jobs found')
  }
}
