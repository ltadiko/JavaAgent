import { test, expect } from '../fixtures/auth.fixture'
import { JobSearchPage } from '../pages/JobSearchPage'
import { JOB_SEARCH } from '../fixtures/test-data'

test.describe('Job Search', () => {

  test('displays job search page heading', async ({ authenticatedPage: page }) => {
    const jobSearch = new JobSearchPage(page)
    await jobSearch.goto()
    await expect(jobSearch.heading).toBeVisible()
  })

  test('search form has query and location inputs', async ({ authenticatedPage: page }) => {
    const jobSearch = new JobSearchPage(page)
    await jobSearch.goto()
    await expect(page.locator('input[placeholder*="Job title"]')).toBeVisible()
    await expect(page.locator('input[placeholder*="Location"]')).toBeVisible()
    await expect(page.getByRole('button', { name: /search/i })).toBeVisible()
  })

  test('search with query and location → shows results or empty state', async ({ authenticatedPage: page }) => {
    const jobSearch = new JobSearchPage(page)
    await jobSearch.goto()
    await jobSearch.search(JOB_SEARCH.query, JOB_SEARCH.location)

    // Wait for loading to finish
    await expect(jobSearch.loadingIndicator).not.toBeVisible({ timeout: 15_000 })

    // Either results or empty state should be shown
    const hasResults = await jobSearch.jobCards.count() > 0
    const hasEmptyState = await jobSearch.emptyState.isVisible()
    expect(hasResults || hasEmptyState).toBeTruthy()
  })

  test('shows result count text', async ({ authenticatedPage: page }) => {
    const jobSearch = new JobSearchPage(page)
    await jobSearch.goto()

    await expect(jobSearch.loadingIndicator).not.toBeVisible({ timeout: 15_000 })
    await expect(page.locator('text=/\\d+ jobs found/')).toBeVisible()
  })
})
