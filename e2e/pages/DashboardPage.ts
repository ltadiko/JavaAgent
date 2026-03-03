import type { Page, Locator } from '@playwright/test'

export class DashboardPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/dashboard')
  }

  get heading() {
    return this.page.getByRole('heading', { name: /dashboard/i })
  }

  get statsCards(): Locator {
    return this.page.locator('.rounded-lg.bg-white.p-6.shadow')
  }

  get recentActivitySection() {
    return this.page.getByText('Recent Activity')
  }

  get quickActionCv() {
    return this.page.getByRole('link', { name: /upload cv/i })
  }

  get quickActionJobs() {
    return this.page.getByRole('link', { name: /search jobs/i })
  }

  get quickActionApplications() {
    return this.page.getByRole('link', { name: /my applications/i })
  }

  get loadingIndicator() {
    return this.page.getByText('Loading...')
  }
}
