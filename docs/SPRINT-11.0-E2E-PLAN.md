# Sprint 11.0 — E2E Testing with Playwright

**Date:** March 1, 2026  
**Sprint Goal:** Full end-to-end test coverage using Playwright

---

## What Was Built

### Infrastructure
- `playwright.config.ts` — Chromium, auto-starts Vite, screenshots/video on failure
- `package.json` — Added `@playwright/test`, `test:e2e`, `test:e2e:ui`, `test:e2e:headed`
- `Makefile` — Added `e2e`, `e2e-ui`, `e2e-headed`, `e2e-setup` targets

### Page Object Models (9 files)
| POM | Selectors | Actions |
|-----|-----------|---------|
| `LoginPage` | email, password, error, heading | `login()`, `goto()` |
| `RegisterPage` | name, email, password, region | `register()`, `goto()` |
| `DashboardPage` | stats cards, activity, quick actions | `goto()` |
| `CvUploadPage` | upload zone, file input, CV list | `uploadFile()`, `goto()` |
| `JobSearchPage` | query, location, results, count | `search()`, `goto()` |
| `JobDetailPage` | title, company, apply button | — |
| `MotivationListPage` | letter cards, empty state | `deleteLetter()`, `goto()` |
| `ApplicationsListPage` | stats, app cards, actions | `submit()`, `withdraw()`, `goto()` |
| `SidebarNav` | all nav links, logout | `navigateTo()` |

### Test Specs (7 files, 34 tests)
| Spec | Tests | Flow |
|------|-------|------|
| `auth.spec.ts` | 7 | Register, Login, Wrong password, Auth guard, Logout |
| `dashboard.spec.ts` | 6 | Stats, Activity, Quick action navigation |
| `cv-upload.spec.ts` | 3 | Upload PDF, CV list, My CVs heading |
| `job-search.spec.ts` | 4 | Search, Results/empty, Result count |
| `motivation-letters.spec.ts` | 3 | List/empty, Helpful message |
| `applications.spec.ts` | 3 | Stats/empty, Search jobs link |
| `navigation.spec.ts` | 8 | All sidebar nav, Active highlight, Logout |

### Fixtures
- `auth.fixture.ts` — `authenticatedPage` fixture, `loginViaUI()`, `registerViaUI()`
- `test-data.ts` — Test users, job search params, timeouts

---

## How to Run

```bash
# 1. Start infrastructure
docker compose up -d

# 2. Start backend
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 3. One-time: install browsers
make e2e-setup

# 4. Run E2E tests
make e2e          # headless
make e2e-ui       # interactive UI
make e2e-headed   # watch browser
```

---

## Build Status
- **Backend:** 239 tests, 0 failures ✅
- **Frontend:** TypeScript check passes, Vite build OK ✅
- **E2E:** 34 test specs created, ready to run against full stack
