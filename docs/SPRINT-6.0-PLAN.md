# Sprint 6.0 — Motivation Letter Generation Plan

**Date:** March 1, 2026  
**Sprint Goal:** Implement AI-powered motivation letter generation based on user CV and job details  
**Estimated Duration:** 2-3 hours

---

## Overview

Sprint 6 implements motivation letter generation using Spring AI + Ollama:
1. MotivationLetter entity with multi-tenant support
2. AI-powered letter generation using CV + Job data
3. Letter customization and editing
4. REST API for letter operations
5. Template management

---

## Tasks

### Task 6.1: MotivationLetter Entity
- **File:** `src/main/java/com/jobagent/jobagent/motivation/model/MotivationLetter.java`
- **Features:**
  - UUID primary key
  - Multi-tenant (tenantId)
  - Links to User, CvDetails, and JobListing
  - Generated content (TEXT)
  - Custom edits by user
  - Status (DRAFT, GENERATED, EDITED, SENT)
  - Timestamps

### Task 6.2: LetterStatus Enum
- **File:** `src/main/java/com/jobagent/jobagent/motivation/model/LetterStatus.java`

### Task 6.3: MotivationLetter Repository
- **File:** `src/main/java/com/jobagent/jobagent/motivation/repository/MotivationLetterRepository.java`
- Find by user, job, tenant
- Find latest for user+job combination
- Pagination support

### Task 6.4: Flyway Migration
- **File:** `src/main/resources/db/migration/V12__create_motivation_letters_table.sql`
- Note: V6 already exists for motivation_letters, verify and update if needed

### Task 6.5: Motivation DTOs
- `GenerateLetterRequest.java` - Request to generate letter
- `MotivationLetterResponse.java` - API response
- `UpdateLetterRequest.java` - User edits

### Task 6.6: MotivationGeneratorAgent
- **File:** `src/main/java/com/jobagent/jobagent/motivation/service/MotivationGeneratorAgent.java`
- Spring AI ChatClient for Ollama
- Prompt template for letter generation
- Combines CV data + Job requirements

### Task 6.7: MotivationLetterService
- **File:** `src/main/java/com/jobagent/jobagent/motivation/service/MotivationLetterService.java`
- Generate new letter
- Get existing letters
- Update letter content
- Delete letter

### Task 6.8: MotivationController
- **File:** `src/main/java/com/jobagent/jobagent/motivation/controller/MotivationController.java`
- `POST /api/v1/motivations/generate` - Generate letter
- `GET /api/v1/motivations` - List user's letters
- `GET /api/v1/motivations/{id}` - Get specific letter
- `PUT /api/v1/motivations/{id}` - Update letter
- `DELETE /api/v1/motivations/{id}` - Delete letter

### Task 6.9: Prompt Template
- **File:** `src/main/resources/prompts/motivation-letter.st`
- Structured prompt for AI generation

### Task 6.10: Unit Tests
- MotivationLetterTest - Entity tests
- MotivationGeneratorAgentTest - AI agent tests
- MotivationLetterServiceTest - Service tests

---

## Data Model

```
┌─────────────────────────────────────────────────────┐
│                 motivation_letters                   │
├─────────────────────────────────────────────────────┤
│ id                 UUID PRIMARY KEY                  │
│ tenant_id          UUID NOT NULL                     │
│ user_id            UUID NOT NULL (FK users)          │
│ cv_id              UUID (FK cv_details)              │
│ job_id             UUID (FK job_listings)            │
│ generated_content  TEXT                              │
│ edited_content     TEXT                              │
│ status             VARCHAR(20) DEFAULT 'DRAFT'       │
│ tone               VARCHAR(50)                       │
│ language           VARCHAR(10) DEFAULT 'en'          │
│ created_at         TIMESTAMP                         │
│ updated_at         TIMESTAMP                         │
├─────────────────────────────────────────────────────┤
│ INDEXES:                                             │
│ - idx_motivation_tenant_id ON (tenant_id)           │
│ - idx_motivation_user_id ON (user_id)               │
│ - idx_motivation_job_id ON (job_id)                 │
└─────────────────────────────────────────────────────┘
```

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/motivations/generate` | Generate new letter |
| GET | `/api/v1/motivations` | List user's letters |
| GET | `/api/v1/motivations/{id}` | Get letter by ID |
| PUT | `/api/v1/motivations/{id}` | Update letter content |
| DELETE | `/api/v1/motivations/{id}` | Delete letter |
| GET | `/api/v1/motivations/job/{jobId}` | Get letters for job |

---

## AI Generation Flow

```
User Request
    │
    ▼
┌─────────────────┐
│ Load CV Data    │ ── CvDetails.parsedJson
└────────┬────────┘
         │
         ▼
���─────────────────┐
│ Load Job Data   │ ── JobListing (title, company, requirements)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Build Prompt    │ ── motivation-letter.st template
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Call Ollama     │ ── Spring AI ChatClient
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Save Letter     │ ── MotivationLetter entity
└─────────────────┘
```

---

## Success Criteria

- [ ] MotivationLetter entity with all required fields
- [ ] Repository with search queries
- [ ] AI generation using Spring AI + Ollama
- [ ] REST API endpoints functional
- [ ] Unit tests passing
- [ ] Multi-tenant isolation verified

---

## Files to Create

| Category | File |
|----------|------|
| **Entity** | MotivationLetter.java, LetterStatus.java |
| **Repository** | MotivationLetterRepository.java |
| **Service** | MotivationLetterService.java, MotivationGeneratorAgent.java |
| **Controller** | MotivationController.java |
| **DTO** | GenerateLetterRequest.java, MotivationLetterResponse.java, UpdateLetterRequest.java |
| **Prompt** | motivation-letter.st (update existing) |
| **Tests** | MotivationLetterTest.java, MotivationGeneratorAgentTest.java, MotivationLetterServiceTest.java |
