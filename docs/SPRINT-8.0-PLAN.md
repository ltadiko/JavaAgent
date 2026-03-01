# Sprint 8.0 — Application Dashboard & Views Plan

**Date:** March 1, 2026  
**Sprint Goal:** Implement dashboard views and application tracking UI support  
**Estimated Duration:** 2 hours

---

## Overview

Sprint 8 provides dashboard and view endpoints to support the UI:
1. Dashboard summary with stats
2. Recent activity feed
3. Application timeline/history
4. Job recommendations based on CV

---

## Tasks

### Task 8.1: DashboardService
- **File:** `src/main/java/com/jobagent/jobagent/dashboard/service/DashboardService.java`
- Aggregate stats from all modules
- Recent applications, jobs, letters

### Task 8.2: Dashboard DTOs
- `DashboardSummary.java` - Overall stats
- `RecentActivity.java` - Activity feed items
- `ApplicationTimeline.java` - Timeline entries

### Task 8.3: DashboardController
- **File:** `src/main/java/com/jobagent/jobagent/dashboard/controller/DashboardController.java`
- GET /api/v1/dashboard - Summary stats
- GET /api/v1/dashboard/activity - Recent activity
- GET /api/v1/dashboard/recommendations - Job recommendations

### Task 8.4: ApplicationEvent Entity
- Track application status changes
- Create audit trail

### Task 8.5: Unit Tests
- DashboardServiceTest

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/dashboard` | Dashboard summary |
| GET | `/api/v1/dashboard/activity` | Recent activity feed |
| GET | `/api/v1/dashboard/recommendations` | Job recommendations |
| GET | `/api/v1/dashboard/timeline/{appId}` | Application timeline |

---

## Dashboard Summary Structure

```json
{
  "user": {
    "name": "John Doe",
    "email": "john@example.com",
    "memberSince": "2026-01-15"
  },
  "cv": {
    "count": 2,
    "latestParsedAt": "2026-03-01T10:00:00Z",
    "skillsCount": 15
  },
  "jobs": {
    "matchesCount": 45,
    "topMatchScore": 92
  },
  "applications": {
    "total": 12,
    "pending": 3,
    "sent": 5,
    "interviews": 2,
    "offers": 1,
    "rejected": 1
  },
  "letters": {
    "count": 8,
    "latestAt": "2026-03-01T09:00:00Z"
  }
}
```

---

## Files to Create

| Category | File |
|----------|------|
| **Service** | DashboardService.java |
| **Controller** | DashboardController.java |
| **DTO** | DashboardSummary.java, RecentActivity.java, UserSummary.java, CvSummary.java, JobsSummary.java, ApplicationsSummary.java, LettersSummary.java |
| **Tests** | DashboardServiceTest.java |
