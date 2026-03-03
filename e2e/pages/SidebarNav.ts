import type { Page } from '@playwright/test'

export class SidebarNav {
  constructor(private page: Page) {}

  async navigateTo(label: string) {
    await this.page.locator('aside').getByRole('link', { name: label }).click()
  }

  get dashboardLink() {
    return this.page.locator('aside').getByRole('link', { name: /dashboard/i })
  }

  get cvLink() {
    return this.page.locator('aside').getByRole('link', { name: /my cv/i })
  }

  get jobsLink() {
    return this.page.locator('aside').getByRole('link', { name: /job search/i })
  }

  get lettersLink() {
    return this.page.locator('aside').getByRole('link', { name: /cover letters/i })
  }

  get applicationsLink() {
    return this.page.locator('aside').getByRole('link', { name: /applications/i })
  }

  get logoutButton() {
    return this.page.getByRole('button', { name: /logout/i })
  }
}
