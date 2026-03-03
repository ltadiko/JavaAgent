import type { Page, Locator } from '@playwright/test'

export class MotivationListPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/motivation-letters')
  }

  get heading() {
    return this.page.getByRole('heading', { name: /cover letters/i })
  }

  get letterCards(): Locator {
    return this.page.locator('.rounded-lg.bg-white.p-4.shadow')
  }

  get emptyState() {
    return this.page.getByText('No cover letters yet')
  }

  async deleteLetter(index: number) {
    await this.letterCards.nth(index).getByText('Delete').click()
  }
}
