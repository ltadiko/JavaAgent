# Sprint 9.0 — Vue.js Frontend Integration

**Date:** March 1, 2026  
**Sprint Goal:** Connect Vue.js frontend to backend APIs for all use cases  
**Estimated Duration:** 3-4 hours

---

## Overview

Sprint 9 wires up the existing Vue.js scaffold to the backend REST APIs:
1. API service modules for each domain
2. Pinia stores for state management
3. Functional views with real data
4. Navigation layout with sidebar

---

## Tasks

### Task 9.1: API Service Modules
- `api/auth.api.ts` — login, register, logout
- `api/cv.api.ts` — upload, list, parse status
- `api/jobs.api.ts` — search, detail, match
- `api/motivation.api.ts` — generate, list, update, delete
- `api/applications.api.ts` — create, submit, list, timeline, stats
- `api/dashboard.api.ts` — summary, activity

### Task 9.2: Pinia Stores
- `stores/auth.ts` — update with login/register/logout
- `stores/dashboard.ts` — dashboard data
- `stores/cv.ts` — CV upload state
- `stores/jobs.ts` — job search state
- `stores/motivation.ts` — letters state
- `stores/applications.ts` — applications state

### Task 9.3: Layout Component
- `components/AppLayout.vue` — sidebar navigation + header
- `components/AppSidebar.vue` — navigation links
- `components/StatsCard.vue` — reusable stat card

### Task 9.4: Auth Views
- `LoginView.vue` — login form
- `RegisterView.vue` — register form

### Task 9.5: Dashboard View
- `DashboardView.vue` — stats cards, recent activity, quick actions

### Task 9.6: CV Views
- `CvUploadView.vue` — upload form, CV list, parsed summary

### Task 9.7: Job Views
- `JobSearchView.vue` — search with filters
- `JobDetailView.vue` — job detail with apply button

### Task 9.8: Motivation Views
- `MotivationListView.vue` — list letters, generate new

### Task 9.9: Application Views
- `ApplicationsListView.vue` — list with filters and stats

---

## Success Criteria
- [ ] All API modules created
- [ ] All Pinia stores created
- [ ] All views functional with API data
- [ ] Navigation between views works
- [ ] Build passes (`npm run build`)
