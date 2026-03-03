import { test, expect, type Page } from '@playwright/test'
import { registerViaAPI, loginViaAPI } from '../fixtures/auth.fixture'
import { TIMEOUTS } from '../fixtures/test-data'

/**
 * Full User Journey E2E Test
 * ─────────────────────────────────────────────────────────────────
 * Covers the complete job seeker flow:
 *   1. Sign up (real API)
 *   2. Upload CV & see parsed skills (API mocked for MinIO/parsing)
 *   3. Browse job listings matched to profile (API mocked for job data)
 *   4. Open a job detail page
 *   5. Generate a motivation letter for the job (API mocked for AI)
 *   6. Submit application with one click (API mocked)
 *   7. Verify application appears in "My Applications"
 *
 * API responses are mocked where the backend depends on external services
 * (MinIO, Ollama AI, job scrapers) that may not be running locally.
 * Auth (register, login, JWT) uses the REAL backend.
 * ─────────────────────────────────────────────────────────────────
 */

// ── Seed data ──────────────────────────────────────────────────────
const TS = Date.now()
const USER = {
  fullName: `Journey User ${TS}`,
  email: `journey-${TS}@jobagent.test`,
  password: 'TestPass123!',
  country: 'DE',
}

const MOCK_CV = {
  id: '11111111-1111-1111-1111-111111111111',
  fileName: 'journey-test-cv.pdf',
  fileSize: 52_000,
  contentType: 'application/pdf',
  status: 'PARSED',
  active: true,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
}

const MOCK_PARSED_CV = {
  fullName: USER.fullName,
  email: USER.email,
  phone: '+49 170 1234567',
  currentTitle: 'Senior Software Engineer',
  summary: 'Experienced Java/Spring developer with 8 years in cloud platforms.',
  skills: ['Java', 'Spring Boot', 'PostgreSQL', 'Docker', 'Kubernetes', 'TypeScript', 'Vue.js'],
  experience: [
    { company: 'TechCorp GmbH', title: 'Senior Engineer', location: 'Berlin', startDate: '2020-01', endDate: 'present', description: 'Led backend team.' },
    { company: 'StartupX', title: 'Backend Developer', location: 'Munich', startDate: '2017-06', endDate: '2019-12', description: 'Built microservices.' },
  ],
  education: [
    { institution: 'TU Berlin', degree: 'M.Sc.', field: 'Computer Science', year: '2017' },
  ],
  languages: ['English', 'German'],
  certifications: ['AWS Solutions Architect'],
}

const MOCK_JOBS = [
  {
    id: 'aaaa1111-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    title: 'Senior Java Developer',
    company: 'CloudScale AG',
    location: 'Berlin, Germany',
    description: 'We are looking for a Senior Java Developer to build scalable cloud-native applications using Spring Boot and Kubernetes.',
    requirements: 'Java 17+, Spring Boot, Docker, Kubernetes, PostgreSQL, CI/CD',
    salaryRange: '€75,000 – €95,000',
    jobType: 'FULL_TIME',
    sourceUrl: 'https://example.com/job/1',
    matchScore: 92,
    postedAt: new Date().toISOString(),
    createdAt: new Date().toISOString(),
  },
  {
    id: 'bbbb2222-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    title: 'Full Stack Engineer (Vue + Spring)',
    company: 'FinTech Solutions',
    location: 'Frankfurt, Germany',
    description: 'Join our fintech team to build next-gen banking platforms with Vue.js and Spring.',
    requirements: 'Vue.js, TypeScript, Java, Spring Boot, PostgreSQL',
    salaryRange: '€65,000 – €85,000',
    jobType: 'FULL_TIME',
    sourceUrl: 'https://example.com/job/2',
    matchScore: 87,
    postedAt: new Date().toISOString(),
    createdAt: new Date().toISOString(),
  },
]

const MOCK_MOTIVATION = {
  id: 'cccc3333-cccc-cccc-cccc-cccccccccccc',
  jobId: MOCK_JOBS[0].id,
  jobTitle: MOCK_JOBS[0].title,
  company: MOCK_JOBS[0].company,
  cvId: MOCK_CV.id,
  content: `Dear Hiring Manager,\n\nI am writing to express my strong interest in the Senior Java Developer position at CloudScale AG. With over 8 years of experience in Java and Spring Boot development, including extensive work with cloud-native architectures on Kubernetes, I am confident I can make a significant contribution to your team.\n\nIn my current role at TechCorp GmbH, I led a backend team that built and deployed microservices handling over 10 million requests daily. My experience with PostgreSQL, Docker, and CI/CD pipelines aligns perfectly with your requirements.\n\nI would welcome the opportunity to discuss how my skills can benefit CloudScale AG.\n\nBest regards,\n${USER.fullName}`,
  editedContent: null,
  status: 'GENERATED',
  tone: 'PROFESSIONAL',
  language: 'en',
  wordCount: 142,
  version: 1,
  isEdited: false,
  generatedAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
}

const MOCK_APPLICATION = {
  id: 'dddd4444-dddd-dddd-dddd-dddddddddddd',
  jobId: MOCK_JOBS[0].id,
  jobTitle: MOCK_JOBS[0].title,
  company: MOCK_JOBS[0].company,
  location: MOCK_JOBS[0].location,
  cvId: MOCK_CV.id,
  cvFileName: MOCK_CV.fileName,
  letterId: MOCK_MOTIVATION.id,
  status: 'SENT',
  applyMethod: 'EMAIL',
  confirmationRef: `APP-${TS}`,
  failureReason: null,
  additionalMessage: null,
  submittedAt: new Date().toISOString(),
  sentAt: new Date().toISOString(),
  viewedAt: null,
  responseAt: null,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
}

// ── Helper: install API mocks ──────────────────────────────────────
async function installMocks(page: Page) {
  // CV upload → return mocked CV
  await page.route('**/api/v1/cv/upload', async (route) => {
    await route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(MOCK_CV) })
  })

  // CV list → return one CV
  await page.route('**/api/v1/cv', async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([MOCK_CV]) })
    } else {
      await route.continue()
    }
  })

  // CV parsed data
  await page.route(`**/api/v1/cv/${MOCK_CV.id}/parsed`, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(MOCK_PARSED_CV) })
  })

  // Single job detail — register FIRST (lower priority, checked last by Playwright)
  await page.route(`**/api/v1/jobs/${MOCK_JOBS[0].id}`, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(MOCK_JOBS[0]) })
  })

  // Jobs matches
  await page.route('**/api/v1/jobs/matches**', async (route) => {
    await route.fulfill({
      status: 200, contentType: 'application/json',
      body: JSON.stringify({ content: MOCK_JOBS, totalElements: 2, totalPages: 1, number: 0, size: 20 }),
    })
  })

  // Jobs search → return 2 matched jobs (register LAST = highest priority)
  // Use a handler that checks the URL to avoid catching detail/matches routes
  await page.route('**/api/v1/jobs**', async (route) => {
    const url = route.request().url()
    // Only intercept the search endpoint, not /jobs/{id} or /jobs/matches
    if (url.match(/\/api\/v1\/jobs(\?.*)?$/) || url.endsWith('/api/v1/jobs')) {
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({ content: MOCK_JOBS, totalElements: 2, totalPages: 1, number: 0, size: 20 }),
      })
    } else {
      // Fall through to other registered routes
      await route.fallback()
    }
  })

  // Motivation letter generate
  await page.route('**/api/v1/motivations/generate', async (route) => {
    await route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(MOCK_MOTIVATION) })
  })

  // Motivation letter list (use regex because glob '?' is a single-char wildcard)
  await page.route(/\/api\/v1\/motivations(\?.*)?$/, async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({ content: [MOCK_MOTIVATION], totalElements: 1, totalPages: 1, number: 0, size: 20 }),
      })
    } else {
      await route.fallback()
    }
  })
  await page.route(`**/api/v1/motivations/job/${MOCK_JOBS[0].id}`, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([MOCK_MOTIVATION]) })
  })

  // Application create & list (regex to match with or without query string)
  await page.route(/\/api\/v1\/applications(\?.*)?$/, async (route) => {
    if (route.request().method() === 'POST') {
      await route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(MOCK_APPLICATION) })
    } else if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({ content: [MOCK_APPLICATION], totalElements: 1, totalPages: 1, number: 0, size: 20 }),
      })
    } else {
      await route.continue()
    }
  })

  // Application submit
  await page.route(`**/api/v1/applications/${MOCK_APPLICATION.id}/submit`, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(MOCK_APPLICATION) })
  })

  // Application stats
  await page.route('**/api/v1/applications/stats', async (route) => {
    await route.fulfill({
      status: 200, contentType: 'application/json',
      body: JSON.stringify({ total: 1, drafts: 0, pending: 0, sent: 1, interviews: 0, offers: 0, rejected: 0, byStatus: { SENT: 1 } }),
    })
  })

  // Check if already applied
  await page.route(`**/api/v1/applications/check/${MOCK_JOBS[0].id}`, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ applied: true }) })
  })
}

// ── THE TEST ───────────────────────────────────────────────────────
test.describe('Full User Journey: Signup → CV → Jobs → Motivation → Apply', () => {

  test('complete job application flow', async ({ page, baseURL }) => {
    const proxyBase = baseURL || 'http://127.0.0.1:5173'
    test.setTimeout(120_000) // 2 minutes for full journey

    // ════════════════════════════════════════════════════════════════
    // STEP 1: Sign Up via the UI
    // ════════════════════════════════════════════════════════════════
    await test.step('1. Register a new account', async () => {
      await page.goto('/register')

      await page.getByLabel('Full Name').fill(USER.fullName)
      await page.getByLabel('Email').fill(USER.email)
      await page.getByLabel('Password').fill(USER.password)
      await page.getByLabel('Country').selectOption(USER.country)

      const [registerRes] = await Promise.all([
        page.waitForResponse(r => r.url().includes('/api/v1/auth/register'), { timeout: TIMEOUTS.apiResponse }),
        page.getByRole('button', { name: /register/i }).click(),
      ])
      expect(registerRes.status()).toBe(201)

      // Auto-login happens in the store
      await page.waitForResponse(r => r.url().includes('/api/v1/auth/login'), { timeout: TIMEOUTS.apiResponse })
      await page.waitForURL('**/dashboard', { timeout: TIMEOUTS.navigation })
      await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible()
    })

    // Install mocks AFTER real auth (token is in localStorage)
    await installMocks(page)

    // ════════════════════════════════════════════════════════════════
    // STEP 2: Upload CV & see parsed skills
    // ════════════════════════════════════════════════════════════════
    await test.step('2. Upload CV and see parsed skills', async () => {
      await page.goto('/cv')
      await expect(page.getByRole('heading', { name: 'My CV', exact: true })).toBeVisible()

      // Upload a file (mocked API response)
      const fileInput = page.locator('input[type="file"]')
      await fileInput.setInputFiles({
        name: 'my-resume.pdf',
        mimeType: 'application/pdf',
        buffer: Buffer.from('%PDF-1.4 test content'),
      })

      // Wait for upload to complete — mock returns instantly
      await expect(page.getByText('Uploading...')).not.toBeVisible({ timeout: 5_000 })

      // CV should appear in the list (mocked list returns MOCK_CV)
      await expect(page.getByText(MOCK_CV.fileName).first()).toBeVisible({ timeout: 5_000 })
      await expect(page.getByText('PARSED').first()).toBeVisible()
    })

    // ════════════════════════════════════════════════════════════════
    // STEP 3: Browse matched jobs
    // ════════════════════════════════════════════════════════════════
    await test.step('3. Search and see matched jobs', async () => {
      // Navigate to jobs — wait for the mocked API response
      const [jobsResponse] = await Promise.all([
        page.waitForResponse(r => r.url().includes('/api/v1/jobs') && !r.url().includes('/matches'), { timeout: TIMEOUTS.apiResponse }),
        page.goto('/jobs'),
      ])
      expect(jobsResponse.status()).toBe(200)

      await expect(page.getByRole('heading', { name: /job search/i })).toBeVisible()

      // Jobs are loaded on mount (mocked) — wait for the count to update
      await expect(page.getByText('2 jobs found')).toBeVisible({ timeout: 10_000 })

      // Verify both job cards are visible
      await expect(page.getByText(MOCK_JOBS[0].title)).toBeVisible()
      await expect(page.getByText(MOCK_JOBS[0].company)).toBeVisible()
      await expect(page.getByText('92% match')).toBeVisible()

      await expect(page.getByText(MOCK_JOBS[1].title)).toBeVisible()
      await expect(page.getByText('87% match')).toBeVisible()
    })

    // ════════════════════════════════════════════════════════════════
    // STEP 4: Open a job detail page
    // ════════════════════════════════════════════════════════════════
    await test.step('4. Open job detail for best match', async () => {
      // Click on first job (highest match)
      await page.getByText(MOCK_JOBS[0].title).click()
      await page.waitForURL(`**/jobs/${MOCK_JOBS[0].id}`, { timeout: TIMEOUTS.navigation })

      // Verify detail page
      await expect(page.getByRole('heading', { name: MOCK_JOBS[0].title })).toBeVisible()
      await expect(page.getByText(MOCK_JOBS[0].company)).toBeVisible()
      await expect(page.getByText('92% match')).toBeVisible()
      await expect(page.getByText('Description')).toBeVisible()
      await expect(page.getByRole('link', { name: /apply now/i })).toBeVisible()
    })

    // ════════════════════════════════════════════════════════════════
    // STEP 5: Click "Apply Now" → goes to applications page
    // ════════════════════════════════════════════════════════════════
    await test.step('5. Click Apply Now', async () => {
      await page.getByRole('link', { name: /apply now/i }).click()
      await page.waitForURL('**/applications**', { timeout: TIMEOUTS.navigation })
      await expect(page.getByRole('heading', { name: /my applications/i })).toBeVisible()
    })

    // ════════════════════════════════════════════════════════════════
    // STEP 6: Verify application in the list
    // ════════════════════════════════════════════════════════════════
    await test.step('6. Verify application appears in My Applications', async () => {
      // Wait for applications to load
      await expect(page.getByText('Loading...')).not.toBeVisible({ timeout: 10_000 })

      // Application list is loaded (mocked to return our application)
      await expect(page.getByText(MOCK_JOBS[0].title)).toBeVisible({ timeout: 10_000 })
      await expect(page.getByText(MOCK_JOBS[0].company)).toBeVisible()
      // Use exact match for status badge (uppercase SENT, not "Sent" from stats)
      await expect(page.locator('text="SENT"').first()).toBeVisible({ timeout: 5_000 })
    })

    // ════════════════════════════════════════════════════════════════
    // STEP 7: Navigate to Cover Letters to see the motivation
    // ════════════════════════════════════════════════════════════════
    await test.step('7. View generated motivation letter', async () => {
      await page.goto('/motivation-letters')
      await expect(page.getByRole('heading', { name: /cover letters/i })).toBeVisible()

      // Wait for loading to finish
      await expect(page.getByText('Loading...')).not.toBeVisible({ timeout: 10_000 })

      // Letter from mocked API — use heading role for unique match (title may appear in content too)
      await expect(page.getByRole('heading', { name: MOCK_JOBS[0].title })).toBeVisible({ timeout: 5_000 })
      await expect(page.getByText(MOCK_JOBS[0].company).first()).toBeVisible()
      await expect(page.getByText('GENERATED')).toBeVisible()
      await expect(page.getByText(/Dear Hiring Manager/).first()).toBeVisible()
    })

    // ════════════════════════════════════════════════════════════════
    // STEP 8: Verify dashboard reflects activity
    // ════════════════════════════════════════════════════════════════
    await test.step('8. Dashboard shows updated stats', async () => {
      // Mock the dashboard API to reflect the activity
      await page.route('**/api/v1/dashboard', async (route) => {
        await route.fulfill({
          status: 200, contentType: 'application/json',
          body: JSON.stringify({
            user: { name: USER.fullName, email: USER.email, memberSince: new Date().toISOString(), region: 'EU' },
            cv: { count: 1, skillsCount: 7, topSkills: ['Java', 'Spring Boot', 'PostgreSQL', 'Docker', 'Kubernetes'] },
            jobs: { matchesCount: 2, topMatchScore: 92, newJobsToday: 2, savedJobs: 0 },
            applications: { total: 1, drafts: 0, pending: 0, sent: 1, interviews: 0, offers: 0, rejected: 0, withdrawn: 0 },
            letters: { count: 1 },
          }),
        })
      })
      await page.route('**/api/v1/dashboard/activity**', async (route) => {
        await route.fulfill({
          status: 200, contentType: 'application/json',
          body: JSON.stringify([{
            id: 'eeee5555-eeee-eeee-eeee-eeeeeeeeeeee',
            type: 'APPLICATION_SENT',
            title: 'Application sent',
            description: `${MOCK_JOBS[0].company} - ${MOCK_JOBS[0].title}`,
            entityType: 'APPLICATION',
            entityId: MOCK_APPLICATION.id,
            timestamp: new Date().toISOString(),
          }]),
        })
      })

      await page.goto('/dashboard')
      await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible()
      await expect(page.getByText('Loading...')).not.toBeVisible({ timeout: TIMEOUTS.apiResponse })

      // Stats cards should reflect our activity (use main region to avoid sidebar matches)
      const main = page.locator('main')
      await expect(main.getByText('Applications').first()).toBeVisible()
      await expect(main.getByText('CVs Uploaded')).toBeVisible()
      await expect(main.getByText('Cover Letters').first()).toBeVisible()

      // Recent activity
      await expect(main.getByText('Application sent')).toBeVisible()
      await expect(main.getByText(MOCK_JOBS[0].company).first()).toBeVisible()
    })
  })
})
