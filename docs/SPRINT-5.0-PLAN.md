# Sprint 5.0 — Job Search Implementation Plan

**Date:** March 1, 2026  
**Sprint Goal:** Implement Job Listing entity, search functionality, and job matching based on CV skills  
**Estimated Duration:** 2-3 hours

---

## Overview

Sprint 5 implements the core job search functionality:
1. JobListing entity with multi-tenant support
2. Job search repository with filtering
3. Job matching service based on parsed CV skills
4. REST API endpoints for job operations
5. Integration with pgvector for semantic search (RAG)

---

## Tasks

### Task 5.1: JobListing Entity
- **File:** `src/main/java/com/jobagent/jobagent/jobsearch/model/JobListing.java`
- **Features:**
  - UUID primary key
  - Multi-tenant (tenantId)
  - Fields: title, company, location, description, requirements, salary range
  - Skills list (JSON column)
  - Source URL and external ID
  - Status (ACTIVE, EXPIRED, APPLIED)
  - Timestamps (createdAt, updatedAt, expiresAt)

### Task 5.2: JobListing Repository
- **File:** `src/main/java/com/jobagent/jobagent/jobsearch/repository/JobListingRepository.java`
- **Features:**
  - Find by tenant
  - Search by title, company, location
  - Find by skills (matching)
  - Find active jobs only
  - Pagination support

### Task 5.3: Flyway Migration
- **File:** `src/main/resources/db/migration/V7__create_job_listings.sql`
- **Features:**
  - job_listings table
  - Indexes for search performance
  - GIN index for skills (JSONB)

### Task 5.4: Job DTOs
- **Files:**
  - `JobListingResponse.java` - API response
  - `JobSearchRequest.java` - Search filters
  - `JobMatchScore.java` - Match result with score

### Task 5.5: JobSearchService
- **File:** `src/main/java/com/jobagent/jobagent/jobsearch/service/JobSearchService.java`
- **Features:**
  - Search jobs with filters
  - Get job by ID
  - Save new job listings
  - Update job status

### Task 5.6: JobMatchingService
- **File:** `src/main/java/com/jobagent/jobagent/jobsearch/service/JobMatchingService.java`
- **Features:**
  - Match jobs to user's CV skills
  - Calculate match score (percentage)
  - Rank jobs by relevance
  - Use parsed CV data from CvParsedData

### Task 5.7: JobSearchController
- **File:** `src/main/java/com/jobagent/jobagent/jobsearch/controller/JobSearchController.java`
- **Endpoints:**
  - `GET /api/v1/jobs` - Search/list jobs
  - `GET /api/v1/jobs/{id}` - Get job details
  - `GET /api/v1/jobs/matches` - Get matched jobs for user
  - `POST /api/v1/jobs` - Create job (admin/scraper)

### Task 5.8: Unit Tests
- JobListingTest - Entity tests
- JobSearchServiceTest - Service layer tests
- JobMatchingServiceTest - Matching algorithm tests

### Task 5.9: Integration Tests
- JobListingRepositoryIntegrationTest
- JobSearchControllerIntegrationTest

---

## Data Model

```
┌─────────────────────────────────────────────────────┐
│                    job_listings                      │
├─────────────────────────────────────────────────────┤
│ id                 UUID PRIMARY KEY                  │
│ tenant_id          UUID NOT NULL                     │
│ title              VARCHAR(500) NOT NULL             │
│ company            VARCHAR(255) NOT NULL             │
│ location           VARCHAR(255)                      │
│ description        TEXT                              │
│ requirements       TEXT                              │
│ skills             JSONB (list of skills)            │
│ salary_min         DECIMAL                           │
│ salary_max         DECIMAL                           │
│ salary_currency    VARCHAR(3)                        │
│ employment_type    VARCHAR(50)                       │
│ remote_type        VARCHAR(50)                       │
│ source_url         VARCHAR(2000)                     │
│ external_id        VARCHAR(255)                      │
│ status             VARCHAR(20) DEFAULT 'ACTIVE'      │
│ created_at         TIMESTAMP                         │
│ updated_at         TIMESTAMP                         │
│ expires_at         TIMESTAMP                         │
├─────────────────────────────────────────────────────┤
│ INDEXES:                                             │
│ - idx_job_tenant_id ON (tenant_id)                  │
│ - idx_job_status ON (status)                        │
│ - idx_job_location ON (location)                    │
│ - idx_job_skills ON (skills) USING GIN             │
│ - idx_job_created_at ON (created_at)               │
└─────────────────────────────────────────────────────┘
```

---

## Matching Algorithm

```
MatchScore = (MatchedSkills / TotalRequiredSkills) * 100

Example:
- Job requires: [Java, Spring, PostgreSQL, Docker, Kubernetes]
- User has: [Java, Spring, PostgreSQL, React]
- Matched: [Java, Spring, PostgreSQL] = 3
- Score: (3/5) * 100 = 60%
```

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/jobs` | Search jobs with filters |
| GET | `/api/v1/jobs/{id}` | Get job details |
| GET | `/api/v1/jobs/matches` | Get jobs matched to user's CV |
| POST | `/api/v1/jobs` | Create job listing |
| PUT | `/api/v1/jobs/{id}/status` | Update job status |

---

## Dependencies

- Spring Data JPA
- PostgreSQL with JSONB
- Existing CV module (for skill matching)
- Multi-tenant filter

---

## Success Criteria

- [ ] JobListing entity with all required fields
- [ ] Repository with search and filter queries
- [ ] Flyway migration executed successfully
- [ ] JobSearchService with CRUD operations
- [ ] JobMatchingService calculates accurate match scores
- [ ] REST API endpoints functional
- [ ] Unit tests passing
- [ ] Integration tests passing
- [ ] Multi-tenant isolation verified

---

## Files to Create

| Category | File |
|----------|------|
| **Entity** | JobListing.java, JobStatus.java |
| **Repository** | JobListingRepository.java |
| **Service** | JobSearchService.java, JobMatchingService.java |
| **Controller** | JobSearchController.java |
| **DTO** | JobListingResponse.java, JobSearchRequest.java, JobMatchScore.java |
| **Migration** | V7__create_job_listings.sql |
| **Tests** | JobListingTest.java, JobSearchServiceTest.java, JobMatchingServiceTest.java |
| **Integration** | JobListingRepositoryIntegrationTest.java |
