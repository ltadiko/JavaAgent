import type { Page, Locator } from '@playwright/test'

export class ApplicationsListPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/applications')
  }

  get heading() {
    return this.page.getByRole('heading', { name: /my applications/i })
  }

  get statsCards(): Locator {
    return this.page.locator('.rounded-lg.bg-white.p-6.shadow')
  }

  get applicationCards(): Locator {
    return this.page.locator('.rounded-lg.bg-white.p-4.shadow')
  }

  get emptyState() {
    return this.page.getByText('No applications yet')
  }

  get searchJobsLink() {
    return this.page.getByRole('link', { name: /search for jobs/i })
  }

  async submitApplication(index: number) {
    await this.applicationCards.nth(index).getByText('Submit').click()
  }

  async withdrawApplication(index: number) {
    await this.applicationCards.nth(index).getByText('Withdraw').click()
  }
}
