import { test, expect } from '../fixtures/auth.fixture'
import { DashboardPage } from '../pages/DashboardPage'

test.describe('Dashboard', () => {

  test('displays dashboard heading', async ({ authenticatedPage: page }) => {
    const dashboard = new DashboardPage(page)
    await dashboard.goto()
    await expect(dashboard.heading).toBeVisible()
  })

  test('shows stats cards or welcome message after loading', async ({ authenticatedPage: page }) => {
    const dashboard = new DashboardPage(page)
    await dashboard.goto()

    // Wait for loading to finish
    await expect(dashboard.loadingIndicator).not.toBeVisible({ timeout: 15_000 })

    // Either stats cards are shown (data loaded) OR welcome message (no data / new user)
    const statsCard = dashboard.statsCards.first()
    const welcome = page.getByText('Welcome to JobAgent')
    await expect(statsCard.or(welcome)).toBeVisible()
  })

  test('shows recent activity or "no activity" message', async ({ authenticatedPage: page }) => {
    const dashboard = new DashboardPage(page)
    await dashboard.goto()
    await expect(dashboard.loadingIndicator).not.toBeVisible({ timeout: 15_000 })

    // Dashboard loaded: either shows "Recent Activity" heading (data loaded) or "Welcome" (no data)
    const activityHeading = page.getByRole('heading', { name: 'Recent Activity' })
    const welcome = page.getByText('Welcome to JobAgent')
    await expect(activityHeading.or(welcome)).toBeVisible()
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
