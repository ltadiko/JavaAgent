import { test, expect } from '../fixtures/auth.fixture'
import { CvUploadPage } from '../pages/CvUploadPage'
import path from 'path'
import fs from 'fs'

test.describe('CV Upload', () => {

  test('displays CV page heading and upload zone', async ({ authenticatedPage: page }) => {
    const cvPage = new CvUploadPage(page)
    await cvPage.goto()
    await expect(cvPage.heading).toBeVisible()
    await expect(cvPage.uploadZone).toBeVisible()
  })

  test('upload a PDF file → appears in CV list', async ({ authenticatedPage: page }) => {
    const cvPage = new CvUploadPage(page)
    await cvPage.goto()

    // Create a temporary test PDF
    const testFile = path.join(__dirname, '..', 'fixtures', 'test-cv.pdf')
    if (!fs.existsSync(testFile)) {
      // Create a minimal PDF file for testing
      fs.writeFileSync(testFile, '%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n2 0 obj<</Type/Pages/Count 1/Kids[3 0 R]>>endobj\n3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R>>endobj\nxref\n0 4\n0000000000 65535 f \n0000000009 00000 n \n0000000058 00000 n \n0000000115 00000 n \ntrailer<</Size 4/Root 1 0 R>>\nstartxref\n190\n%%EOF')
    }

    await cvPage.uploadFile(testFile)

    // Wait for upload to complete
    await expect(cvPage.uploadingIndicator).not.toBeVisible({ timeout: 20_000 })

    // CV should appear in the list
    await expect(cvPage.cvList.first()).toBeVisible({ timeout: 10_000 })
  })

  test('CV list section shows "My CVs" heading', async ({ authenticatedPage: page }) => {
    const cvPage = new CvUploadPage(page)
    await cvPage.goto()
    await expect(page.getByText('My CVs')).toBeVisible()
  })
})
