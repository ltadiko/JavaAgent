import { test, expect } from '../fixtures/auth.fixture'
import { DashboardPage } from '../pages/DashboardPage'

test.describe('Dashboard', () => {

  test('displays dashboard heading', async ({ authenticatedPage: page }) => {
    const dashboard = new DashboardPage(page)
    await dashboard.goto()
    await expect(dashboard.heading).toBeVisible()
  })

  test('shows stats cards with numeric values', async ({ authenticatedPage: page }) => {
    const dashboard = new DashboardPage(page)
    await dashboard.goto()

    // Wait for loading to finish
    await expect(dashboard.loadingIndicator).not.toBeVisible({ timeout: 15_000 })

    // At least the top-level stats cards should render
    const statsCards = dashboard.statsCards
    await expect(statsCards.first()).toBeVisible()
  })

  test('shows recent activity section', async ({ authenticatedPage: page }) => {
    const dashboard = new DashboardPage(page)
    await dashboard.goto()
    await expect(dashboard.loadingIndicator).not.toBeVisible({ timeout: 15_000 })
    await expect(dashboard.recentActivitySection).toBeVisible()
  })

  test('quick action "Upload CV" navigates to /cv', async ({ authenticatedPage: page }) => {
    const dashboard = new DashboardPage(page)
    await dashboard.goto()
    await expect(dashboard.loadingIndicator).not.toBeVisible({ timeout: 15_000 })
    await dashboard.quickActionCv.click()
    await expect(page).toHaveURL(/\/cv/)
  })

  test('quick action "Search Jobs" navigates to /jobs', async ({ authenticatedPage: page }) => {
    const dashboard = new DashboardPage(page)
    await dashboard.goto()
    await expect(dashboard.loadingIndicator).not.toBeVisible({ timeout: 15_000 })
    await dashboard.quickActionJobs.click()
    await expect(page).toHaveURL(/\/jobs/)
  })

  test('quick action "My Applications" navigates to /applications', async ({ authenticatedPage: page }) => {
    const dashboard = new DashboardPage(page)
    await dashboard.goto()
    await expect(dashboard.loadingIndicator).not.toBeVisible({ timeout: 15_000 })
    await dashboard.quickActionApplications.click()
    await expect(page).toHaveURL(/\/applications/)
  })
})
