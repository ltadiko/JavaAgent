import { test, expect } from '../fixtures/auth.fixture'
import { MotivationListPage } from '../pages/MotivationListPage'

test.describe('Motivation Letters', () => {

  test('displays cover letters heading', async ({ authenticatedPage: page }) => {
    const letters = new MotivationListPage(page)
    await letters.goto()
    await expect(letters.heading).toBeVisible()
  })

  test('shows letters or empty state', async ({ authenticatedPage: page }) => {
    const letters = new MotivationListPage(page)
    await letters.goto()

    // Wait for content to load
    await page.waitForTimeout(3_000)

    const hasLetters = await letters.letterCards.count() > 0
    const hasEmptyState = await letters.emptyState.isVisible()
    expect(hasLetters || hasEmptyState).toBeTruthy()
  })

  test('empty state shows helpful message', async ({ authenticatedPage: page }) => {
    const letters = new MotivationListPage(page)
    await letters.goto()

    // If empty, should show guidance
    if (await letters.emptyState.isVisible()) {
      await expect(page.getByText('Generate a cover letter from a job detail page')).toBeVisible()
    }
  })
})
