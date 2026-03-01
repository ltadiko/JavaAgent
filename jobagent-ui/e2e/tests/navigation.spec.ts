import { test, expect } from '../fixtures/auth.fixture'
import { SidebarNav } from '../pages/SidebarNav'

test.describe('Navigation', () => {

  test('sidebar has all navigation links', async ({ authenticatedPage: page }) => {
    await page.goto('/dashboard')
    const sidebar = new SidebarNav(page)

    await expect(sidebar.dashboardLink).toBeVisible()
    await expect(sidebar.cvLink).toBeVisible()
    await expect(sidebar.jobsLink).toBeVisible()
    await expect(sidebar.lettersLink).toBeVisible()
    await expect(sidebar.applicationsLink).toBeVisible()
  })

  test('sidebar → Dashboard', async ({ authenticatedPage: page }) => {
    await page.goto('/jobs')
    const sidebar = new SidebarNav(page)
    await sidebar.navigateTo('Dashboard')
    await expect(page).toHaveURL(/\/dashboard/)
  })

  test('sidebar → My CV', async ({ authenticatedPage: page }) => {
    await page.goto('/dashboard')
    const sidebar = new SidebarNav(page)
    await sidebar.navigateTo('My CV')
    await expect(page).toHaveURL(/\/cv/)
  })

  test('sidebar → Job Search', async ({ authenticatedPage: page }) => {
    await page.goto('/dashboard')
    const sidebar = new SidebarNav(page)
    await sidebar.navigateTo('Job Search')
    await expect(page).toHaveURL(/\/jobs/)
  })

  test('sidebar → Cover Letters', async ({ authenticatedPage: page }) => {
    await page.goto('/dashboard')
    const sidebar = new SidebarNav(page)
    await sidebar.navigateTo('Cover Letters')
    await expect(page).toHaveURL(/\/motivation-letters/)
  })

  test('sidebar → Applications', async ({ authenticatedPage: page }) => {
    await page.goto('/dashboard')
    const sidebar = new SidebarNav(page)
    await sidebar.navigateTo('Applications')
    await expect(page).toHaveURL(/\/applications/)
  })

  test('active sidebar link is highlighted', async ({ authenticatedPage: page }) => {
    await page.goto('/dashboard')
    const sidebar = new SidebarNav(page)

    // Dashboard link should have active styling
    await expect(sidebar.dashboardLink).toHaveClass(/bg-blue-50/)
  })

  test('logout button visible in header', async ({ authenticatedPage: page }) => {
    await page.goto('/dashboard')
    const sidebar = new SidebarNav(page)
    await expect(sidebar.logoutButton).toBeVisible()
  })
})
