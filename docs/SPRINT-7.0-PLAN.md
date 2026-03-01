# Sprint 7.0 — Job Application Submission Plan

**Date:** March 1, 2026  
**Sprint Goal:** Implement job application submission with automatic sending  
**Estimated Duration:** 2-3 hours

---

## Overview

Sprint 7 implements the job application submission flow:
1. Application entity linking user, job, CV, and motivation letter
2. Application status tracking
3. Application submission service
4. Email sending (simulated for MVP)
5. REST API for application management

---

## Tasks

### Task 7.1: ApplicationStatus Enum
- **File:** `src/main/java/com/jobagent/jobagent/application/model/ApplicationStatus.java`
- Values: DRAFT, SUBMITTED, SENT, VIEWED, REJECTED, INTERVIEW, ACCEPTED

### Task 7.2: JobApplication Entity
- **File:** `src/main/java/com/jobagent/jobagent/application/model/JobApplication.java`
- Links to User, JobListing, CvDetails, MotivationLetter
- Multi-tenant support
- Timestamps for submission, sent, viewed

### Task 7.3: JobApplicationRepository
- **File:** `src/main/java/com/jobagent/jobagent/application/repository/JobApplicationRepository.java`
- Find by user, job, status
- Pagination support

### Task 7.4: Application DTOs
- `SubmitApplicationRequest.java`
- `JobApplicationResponse.java`
- `ApplicationStatusUpdate.java`

### Task 7.5: ApplicationService
- **File:** `src/main/java/com/jobagent/jobagent/application/service/ApplicationService.java`
- Create application
- Submit application
- Track status
- List applications

### Task 7.6: ApplicationSenderService
- **File:** `src/main/java/com/jobagent/jobagent/application/service/ApplicationSenderService.java`
- Async sending
- Email simulation (logging for MVP)
- Status updates

### Task 7.7: ApplicationController
- **File:** `src/main/java/com/jobagent/jobagent/application/controller/ApplicationController.java`
- POST /api/v1/applications - Create application
- POST /api/v1/applications/{id}/submit - Submit application
- GET /api/v1/applications - List applications
- GET /api/v1/applications/{id} - Get application
- PUT /api/v1/applications/{id}/status - Update status
- DELETE /api/v1/applications/{id} - Delete draft

### Task 7.8: Unit Tests
- JobApplicationTest
- ApplicationServiceTest

---

## Data Model

```
┌─────────────────────────────────────────────────────┐
│                  job_applications                    │
├─────────────────────────────────────────────────────┤
│ id                 UUID PRIMARY KEY                  │
│ tenant_id          UUID NOT NULL                     │
│ user_id            UUID NOT NULL (FK users)          │
│ job_id             UUID NOT NULL (FK job_listings)   │
│ cv_id              UUID (FK cv_details)              │
│ letter_id          UUID (FK motivation_letters)      │
│ status             VARCHAR(20) DEFAULT 'DRAFT'       │
│ cover_note         TEXT                              │
│ applied_at         TIMESTAMP                         │
│ sent_at            TIMESTAMP                         │
│ viewed_at          TIMESTAMP                         │
│ response_at        TIMESTAMP                         │
│ created_at         TIMESTAMP                         │
│ updated_at         TIMESTAMP                         │
├─────────────────────────────────────────────────────┤
│ UNIQUE: (user_id, job_id, tenant_id)                │
└─────────────────────────────────────────────────────┘
```

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/applications` | Create draft application |
| POST | `/api/v1/applications/{id}/submit` | Submit application |
| GET | `/api/v1/applications` | List user's applications |
| GET | `/api/v1/applications/{id}` | Get application details |
| PUT | `/api/v1/applications/{id}/status` | Update status |
| DELETE | `/api/v1/applications/{id}` | Delete draft |
| GET | `/api/v1/applications/stats` | Get application stats |

---

## Application Flow

```
User selects Job + CV + Letter
         │
         ▼
┌─────────────────┐
│ Create Draft    │ ── Status: DRAFT
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Submit          │ ── Status: SUBMITTED
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Send (Async)    │ ── Email/Portal submission
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Status: SENT    │ ── Waiting for response
└────────┬────────┘
         │
         ▼
   ┌─────┴─────┐
   │           │
VIEWED    REJECTED    INTERVIEW    ACCEPTED
```

---

## Success Criteria

- [ ] JobApplication entity with all fields
- [ ] Repository with search queries
- [ ] Application creation and submission
- [ ] Async sending simulation
- [ ] REST API endpoints
- [ ] Unit tests passing
- [ ] Multi-tenant isolation

---

## Files to Create

| Category | File |
|----------|------|
| **Entity** | JobApplication.java, ApplicationStatus.java |
| **Repository** | JobApplicationRepository.java |
| **Service** | ApplicationService.java, ApplicationSenderService.java |
| **Controller** | ApplicationController.java |
| **DTO** | SubmitApplicationRequest.java, JobApplicationResponse.java, ApplicationStatusUpdate.java |
| **Migration** | V12__create_job_applications.sql |
| **Tests** | JobApplicationTest.java, ApplicationServiceTest.java |
