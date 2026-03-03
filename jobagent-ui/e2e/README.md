# E2E Testing with Playwright — JobAgent

## Overview

End-to-end tests using [Playwright](https://playwright.dev/) covering all UI flows against the full local stack.

## Test Coverage

| Test File | Flow | Tests |
|-----------|------|-------|
| `auth.spec.ts` | Register, Login, Logout, Auth guard | 7 |
| `dashboard.spec.ts` | Stats, Activity, Quick actions | 6 |
| `cv-upload.spec.ts` | Upload PDF, CV list | 3 |
| `job-search.spec.ts` | Search, Results, Empty state | 4 |
| `motivation-letters.spec.ts` | Letters list, Empty state | 3 |
| `applications.spec.ts` | Stats, List, Empty state | 3 |
| `navigation.spec.ts` | Sidebar, Active link, Logout | 8 |
| **Total** | | **34** |

## Architecture

```
e2e/
├── fixtures/           # Test data & auth helpers
│   ├── auth.fixture.ts      # Login/Register helpers, authenticatedPage fixture
│   └── test-data.ts         # Constants (users, timeouts)
├── pages/              # Page Object Models
│   ├── LoginPage.ts
│   ├── RegisterPage.ts
│   ├── DashboardPage.ts
│   ├── CvUploadPage.ts
│   ├── JobSearchPage.ts
│   ├── JobDetailPage.ts
│   ├── MotivationListPage.ts
│   ├── ApplicationsListPage.ts
│   └── SidebarNav.ts
└── tests/              # Test specs
    ├── auth.spec.ts
    ├── dashboard.spec.ts
    ├── cv-upload.spec.ts
    ├── job-search.spec.ts
    ├── motivation-letters.spec.ts
    ├── applications.spec.ts
    ├── navigation.spec.ts
    └── full-journey.spec.ts     # ⭐ Complete user journey
```

## Prerequisites

1. **Docker infrastructure** running:
   ```bash
   docker compose up -d
   ```

2. **Spring Boot backend** on port 8080:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

3. **Playwright browsers** installed (one-time):
   ```bash
   make e2e-setup
   ```

## Running Tests

```bash
# Headless (CI mode)
make e2e

# Interactive UI mode (debug/develop tests)
make e2e-ui

# Headed mode (see browser)
make e2e-headed
```

Or directly:
```bash
cd jobagent-ui
npm run test:e2e           # headless
npm run test:e2e:ui        # interactive
npm run test:e2e:headed    # headed
```

## Configuration

See `jobagent-ui/playwright.config.ts`:
- **Base URL:** `http://localhost:5173`
- **Browser:** Chromium
- **Timeout:** 30s per test
- **Web server:** Auto-starts Vite (`npm run dev`) if not running
- **Artifacts:** Screenshots on failure, video on retry, trace on retry

## Page Object Model Pattern

Each page has a class in `e2e/pages/` with:
- Locators for key elements (headings, buttons, lists)
- Action methods (`login()`, `search()`, `uploadFile()`)
- Selectors use user-facing text and roles (resilient to CSS changes)

## Auth Fixture

The `authenticatedPage` fixture in `auth.fixture.ts`:
1. Navigates to `/login`
2. Fills email/password
3. Clicks "Sign In"
4. Waits for `/dashboard` redirect
5. Provides the logged-in page to the test

Tests that need auth import from `../fixtures/auth.fixture` instead of `@playwright/test`.
