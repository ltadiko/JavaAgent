import { test, expect } from '../fixtures/auth.fixture'
import { ApplicationsListPage } from '../pages/ApplicationsListPage'

test.describe('Applications', () => {

  test('displays applications heading', async ({ authenticatedPage: page }) => {
    const apps = new ApplicationsListPage(page)
    await apps.goto()
    await expect(apps.heading).toBeVisible()
  })

  test('shows stats cards or empty state', async ({ authenticatedPage: page }) => {
    const apps = new ApplicationsListPage(page)
    await apps.goto()

    await page.waitForTimeout(3_000)

    const hasCards = await apps.statsCards.count() > 0
    const hasEmptyState = await apps.emptyState.isVisible()
    expect(hasCards || hasEmptyState).toBeTruthy()
  })

  test('empty state has "Search for jobs" link', async ({ authenticatedPage: page }) => {
    const apps = new ApplicationsListPage(page)
    await apps.goto()

    if (await apps.emptyState.isVisible()) {
      await expect(apps.searchJobsLink).toBeVisible()
      await apps.searchJobsLink.click()
      await expect(page).toHaveURL(/\/jobs/)
    }
  })
})
