import type { Page, Locator } from '@playwright/test'

export class CvUploadPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/cv')
  }

  get heading() {
    return this.page.getByRole('heading', { name: 'My CV', exact: true })
  }

  get uploadZone() {
    return this.page.locator('.border-dashed')
  }

  get fileInput() {
    return this.page.locator('input[type="file"]')
  }

  async uploadFile(filePath: string) {
    await this.fileInput.setInputFiles(filePath)
  }

  get cvList(): Locator {
    return this.page.locator('.rounded.border.p-3')
  }

  get uploadingIndicator() {
    return this.page.getByText('Uploading...')
  }

  get errorMessage() {
    return this.page.locator('.text-red-600')
  }

  get emptyState() {
    return this.page.getByText('No CVs uploaded yet')
  }
}
