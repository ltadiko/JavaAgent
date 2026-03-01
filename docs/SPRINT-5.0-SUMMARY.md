# Sprint 5.0 Summary — Job Search Core

**Sprint:** 5.0  
**Date:** March 1, 2026  
**Status:** ✅ Complete  

---

## Sprint Goal

Implement core job search functionality with skill-based matching against user CVs.

---

## Delivered Features

### 1. Job Listing Management
- Create, read, update job listings
- Multi-tenant isolation
- Status management (ACTIVE, EXPIRED, APPLIED, FILLED)
- Expiration handling

### 2. Job Search
- Full-text search using PostgreSQL tsvector
- Filter by title, company, location
- Skills-based filtering with JSONB
- Pagination support

### 3. Job-CV Matching
- Match jobs to user's parsed CV skills
- Calculate match percentage
- Advanced skill matching:
  - Exact: "java" == "java"
  - Partial: "spring boot" contains "spring"
  - Aliases: "js" → "javascript", "k8s" → "kubernetes"
- Sort by match score
- Filter by minimum match percentage

### 4. REST API
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/jobs` | GET | List active jobs |
| `/api/v1/jobs/search` | POST | Search with filters |
| `/api/v1/jobs/{id}` | GET | Get job details |
| `/api/v1/jobs/matches` | GET | Get CV-matched jobs |
| `/api/v1/jobs/top-matches` | GET | Get top N matches |
| `/api/v1/jobs/{id}/match` | GET | Match score for job |
| `/api/v1/jobs` | POST | Create job |
| `/api/v1/jobs/{id}/status` | PUT | Update status |

---

## Files Created

| Type | Count | Files |
|------|-------|-------|
| Entity | 4 | JobListing, JobStatus, EmploymentType, RemoteType |
| Repository | 1 | JobListingRepository |
| Service | 2 | JobSearchService, JobMatchingService |
| Controller | 1 | JobSearchController |
| DTO | 4 | JobListingResponse, JobSearchRequest, JobMatchScore, CreateJobRequest |
| Migration | 1 | V7__create_job_listings.sql |
| Tests | 3 | JobListingTest, JobSearchServiceTest, JobMatchingServiceTest |

**Total: 16 files**

---

## Test Summary

| Test Class | Tests | Description |
|------------|-------|-------------|
| JobListingTest | 15 | Entity tests (builder, expiration, skills) |
| JobSearchServiceTest | 8 | Service tests (search, create, update) |
| JobMatchingServiceTest | 10 | Matching tests (scoring, aliases) |

**Total: 33 tests**

---

## Database Changes

New table: `job_listings`
```sql
- id (UUID PK)
- tenant_id (UUID, indexed)
- title, company, location
- description, requirements (TEXT)
- skills (JSONB with GIN index)
- salary_min, salary_max, salary_currency
- employment_type, remote_type
- source_url, external_id
- status, created_at, updated_at, expires_at
```

Indexes:
- GIN index on skills for JSONB queries
- Full-text search index on title+description
- Composite index on tenant_id + status

---

## Integration Points

- **CV Module:** Reads parsed CV skills from CvDetails.parsedJson
- **Auth Module:** Uses JWT for user identification in matching
- **Multi-tenancy:** TenantEntityListener for automatic tenant assignment

---

## Next Sprint Preview

**Sprint 6: Motivation Letter Generation**
- MotivationLetter entity
- AI-powered letter generation
- Template management
- Integration with job details and CV

---

## Commands to Run

```bash
# Run Sprint 5 tests
mvn test -Dtest="JobListingTest,JobSearchServiceTest,JobMatchingServiceTest"

# Run all tests
mvn test

# Start application
docker-compose up -d
mvn spring-boot:run -Dspring.profiles.active=local
```

---

## Notes

- Implementation focuses on core functionality
- Job scraping deferred to future sprint
- Vector embeddings (pgvector) integration planned for Sprint 5b
