# Sprint Backlog — Detailed Implementation Plan

> **Philosophy:** Small, testable tasks. Each sprint delivers one working piece.  
> **Rule:** Implement → Test → Fix → Commit → Next.  
> **Convention:** Each task = 1 commit. Run `./mvnw test -Dtest=<TestName>` to verify.

---

## ✅ Sprint 0 — Foundation (DONE)
- Architecture docs, Docker Compose, common module, Flyway V1-V8, package stubs
- **Verified:** App starts, health UP, 18 tables, 13 RLS policies

### Existing Files (Sprint 0)
```
src/main/java/com/jobagent/jobagent/
├── JavaAgentApplication.java
├── common/
│   ├── config/
│   │   ├── AppProperties.java            — @ConfigurationProperties
│   │   ├── CorsConfig.java               — CORS filter
│   │   ├── RootController.java           — GET / welcome endpoint
│   │   └── StorageConfig.java            — MinIO bean config
│   ├── encryption/
│   │   └── EncryptedStringConverter.java  — AES-256-GCM (Sprint 10.4)
│   ├── event/
│   │   ├── BaseEvent.java                — Kafka base event
│   │   ├── EventPublisher.java           — Kafka event publisher
│   │   └── Topics.java                   — Kafka topic names
│   ├── exception/
│   │   ├── DuplicateResourceException.java
│   │   ├── GlobalExceptionHandler.java   — @ControllerAdvice
│   │   └── ResourceNotFoundException.java
│   ├── model/
│   │   └── BaseEntity.java               — id, tenant_id, timestamps
│   ├── multitenancy/
│   │   ├── TenantContext.java            — ThreadLocal tenant holder
│   │   ├── TenantContextFilter.java      — Servlet filter
│   │   └── TenantEntityListener.java     — JPA @PrePersist/@PreUpdate
│   ├── rag/
│   │   ├── RagConstants.java             — doc_type constants
│   │   └── RagSearchHelper.java          — PgVectorStore search wrapper
│   └── security/
│       └── SecurityConfig.java           — SecurityFilterChain (permitAll TODO)
├── auth/
│   ├── model/
│   │   └── User.java                     — ✅ Sprint 1.1
│   └── package-info.java
├── cv/
│   └── package-info.java
├── jobsearch/
│   └── package-info.java
└── motivation/
    └── package-info.java
```

---

## Sprint 1 — User Entity + Registration Endpoint (no auth yet)

### ✅ 1.1 User JPA Entity (DONE)
- ✅ Create `User.java` entity in `auth/model` package
- ✅ Map to `users` table, use `BaseEntity`, `@EntityListeners(TenantEntityListener)`
- ✅ Store `email` as plaintext + `email_hash` (SHA-256) for lookups
- ✅ **Note:** Email encryption postponed to Sprint 10.4 (production hardening)
- ✅ **Test:** 11 unit tests passed, 8 integration tests written
- 📄 **Report:** `docs/SPRINT-1.1-REPORT.md`

---

### ✅ 1.2 UserProfile JPA Entity (DONE)

**Goal:** Create entity for storing user job preferences and PII.

**File:** `src/main/java/com/jobagent/jobagent/auth/model/UserProfile.java`
**Test:** `src/test/java/com/jobagent/jobagent/auth/model/UserProfileTest.java` — 9 tests ✅

**Entity Mapping:**
```java
@Entity
@Table(name = "user_profiles")
@EntityListeners(TenantEntityListener.class)
public class UserProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String phone;                    // Plaintext (Sprint 10: encrypted)
    private String address;                  // Plaintext (Sprint 10: encrypted)
    private String linkedinUrl;
    private String[] preferredJobTitles;     // PostgreSQL TEXT[]
    private String[] preferredLocations;     // PostgreSQL TEXT[]
    private Boolean preferredRemote;         // Default: false
    private Long preferredSalaryMin;
    private String preferredCurrency;        // EUR, USD, GBP (3 chars)
    private String[] preferredLanguages;     // PostgreSQL TEXT[]
}
```

**Sub-tasks:**
- [ ] Create `UserProfile.java` with Lombok `@Builder`, `@Getter`, `@Setter`
- [ ] Add `@OneToOne(fetch = LAZY)` → `User`
- [ ] Handle PostgreSQL TEXT[] arrays using `@JdbcTypeCode(SqlTypes.ARRAY)`
- [ ] Set `preferredRemote` default to `false`

**Test (Unit):** `UserProfileTest.java`
- [ ] Should create empty UserProfile with NoArgsConstructor
- [ ] Should build UserProfile with Builder
- [ ] Should link to User entity via setter
- [ ] Should set default preferredRemote to false
- [ ] Should accept array fields (preferredJobTitles, preferredLocations)
- [ ] Should accept null optional fields (phone, address, linkedinUrl)
- [ ] Should extend BaseEntity (tenant_id, timestamps)

**Test (Integration):** `UserProfileIntegrationTest.java`
- [ ] Should persist UserProfile to database
- [ ] Should load User via @OneToOne relationship
- [ ] Should persist array fields (TEXT[])
- [ ] Should cascade from User deletion (ON DELETE CASCADE)

**Acceptance Criteria:**
- `./mvnw test -Dtest=UserProfileTest` → all pass
- `./mvnw clean compile` → no errors

---

### ✅ 1.3 UserRepository + UserProfileRepository (DONE)

**Goal:** Spring Data JPA repositories with custom query methods.

**Files:**
- `src/main/java/com/jobagent/jobagent/auth/repository/UserRepository.java` ✅
- `src/main/java/com/jobagent/jobagent/auth/repository/UserProfileRepository.java` ✅
- `src/test/.../auth/repository/UserRepositoryIntegrationTest.java` ✅ (5 tests, requires Docker)
- `src/test/.../auth/repository/UserProfileRepositoryIntegrationTest.java` ✅ (3 tests, requires Docker)

**File:** `src/main/java/com/jobagent/jobagent/auth/repository/UserRepository.java`
```java
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailHash(String emailHash);
    boolean existsByEmailHash(String emailHash);
}
```

**File:** `src/main/java/com/jobagent/jobagent/auth/repository/UserProfileRepository.java`
```java
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByUserId(UUID userId);
}
```

**Sub-tasks:**
- [ ] Create `UserRepository.java` interface
- [ ] Create `UserProfileRepository.java` interface

**Test (Integration):** `UserRepositoryIntegrationTest.java`
- [ ] `save()` → generates UUID id
- [ ] `findByEmailHash()` with existing hash → returns user
- [ ] `findByEmailHash()` with unknown hash → returns empty
- [ ] `existsByEmailHash()` with existing hash → true
- [ ] `existsByEmailHash()` with unknown hash → false

**Acceptance Criteria:**
- `./mvnw clean compile` → no errors

---

### 1.4 RegisterRequest DTO + Validation

**Goal:** Immutable request/response DTOs with Jakarta Validation.

**File:** `src/main/java/com/jobagent/jobagent/auth/dto/RegisterRequest.java`
```java
public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 128) String password,
    @NotBlank @Size(min = 2, max = 255) String fullName,
    @NotBlank @Size(min = 2, max = 2) String country
) {}
```

**File:** `src/main/java/com/jobagent/jobagent/auth/dto/RegisterResponse.java`
```java
public record RegisterResponse(
    UUID userId, String email, String fullName,
    String country, String region, Instant createdAt
) {}
```

**Test (Unit):** `RegisterRequestValidationTest.java`
- [ ] Valid request → no violations
- [ ] Blank email → violation
- [ ] Invalid email format → violation
- [ ] Short password (< 8 chars) → violation
- [ ] Blank full name → violation
- [ ] Country too short/long → violation
- [ ] Multiple violations at once → all reported

**Acceptance Criteria:**
- `./mvnw test -Dtest=RegisterRequestValidationTest` → all pass

---

### 1.5 UserService — Registration Logic

**Goal:** Business logic for user registration.

**File:** `src/main/java/com/jobagent/jobagent/auth/service/UserService.java`
- `register(RegisterRequest)` → hash email (SHA-256), check duplicate, hash password (BCrypt), resolve region, save User + UserProfile, return response

**File:** `src/main/java/com/jobagent/jobagent/auth/service/RegionResolver.java`
- `resolveRegion(String countryCode)` → maps ISO country to region (EU/US/APAC/LATAM/MENA)

**Country-to-Region Mapping:**
| Region | Countries |
|--------|-----------|
| EU | DE, FR, NL, BE, IT, ES, PT, AT, IE, FI, SE, DK, PL, CZ, RO, GR, HU, BG, HR, SK, SI, LT, LV, EE, CY, MT, LU, CH, NO, GB |
| US | US, CA |
| APAC | JP, KR, CN, IN, AU, NZ, SG, MY, TH, PH, ID, VN, TW, HK |
| LATAM | BR, MX, AR, CO, CL, PE |
| MENA | AE, SA, EG, IL, TR, QA, KW, BH, OM |

**Dependencies Needed:**
- `PasswordEncoder` bean (BCryptPasswordEncoder) — add `@Bean` in config

**Test (Unit):** `RegionResolverTest.java`
- [ ] DE → EU, US → US, JP → APAC, BR → LATAM, AE → MENA
- [ ] Unknown country → EU (default)
- [ ] Case-insensitive ("de" → EU)

**Test (Unit):** `UserServiceTest.java` (mock repos & PasswordEncoder)
- [ ] Happy path → RegisterResponse with valid UUID, email, region
- [ ] Duplicate email → throws `DuplicateResourceException`
- [ ] Password encoded (verify `passwordEncoder.encode()` called)
- [ ] Email hash is SHA-256 of lowercase trimmed email
- [ ] UserProfile created and linked to User
- [ ] Tenant ID propagated from User to UserProfile

**Acceptance Criteria:**
- `./mvnw test -Dtest=UserServiceTest` → all pass
- `./mvnw test -Dtest=RegionResolverTest` → all pass

---

### ✅ 1.6 Registration REST Controller (DONE)

**Goal:** HTTP endpoint for user registration.

**File:** `src/main/java/com/jobagent/jobagent/auth/controller/AuthController.java` ✅
- `POST /api/v1/auth/register` → `@Valid @RequestBody RegisterRequest` → 201 + `RegisterResponse`

**Infrastructure Fix:** `TenantContextFilter.java` — Added `@ConditionalOnBean(DataSource.class)` to prevent `@WebMvcTest` context failures.

**Error Responses:**
| Status | Condition | Body |
|--------|-----------|------|
| 201 | Success | `RegisterResponse` |
| 400 | Validation failure | `{ "status": 400, "errors": [{ "field": "email", "message": "..." }] }` |
| 409 | Duplicate email | `{ "status": 409, "message": "User already exists..." }` |

**Test (Integration):** `AuthControllerIntegrationTest.java` (standalone MockMvc) — 8 tests ✅
- [x] Valid request → 201 + body has `userId`, `email`, `region`
- [x] Invalid email → 400
- [x] Short password → 400
- [x] Missing fullName → 400
- [x] Invalid country → 400
- [x] Duplicate email → 409
- [x] Response Content-Type is `application/json`
- [x] Empty body → 400

**Acceptance Criteria:**
- ✅ `./mvnw test -Dtest=AuthControllerIntegrationTest` → 8 tests pass
- Manual: `curl -X POST localhost:8080/api/v1/auth/register -H 'Content-Type: application/json' -d '{"email":"test@example.com","password":"password123","fullName":"Test User","country":"US"}'` → 201

---

## Sprint 2 — Spring Authorization Server (JWT auth)

### 2.1 OAuth2 Registered Client Entity

**Goal:** JPA entity for OAuth2 client registration (persisted in database).

**File:** `src/main/java/com/jobagent/jobagent/auth/model/OAuth2RegisteredClient.java`
- Map to `oauth2_registered_client` table (V2 migration)
- Fields: `id`, `clientId`, `clientSecret`, `clientName`, `clientAuthenticationMethods`, `authorizationGrantTypes`, `redirectUris`, `scopes`, `clientSettings`, `tokenSettings`
- **No BaseEntity** — uses Spring Authorization Server's own schema

**Test (Unit):** `OAuth2RegisteredClientTest.java`
- [ ] Entity creates with all fields
- [ ] Getter/setter round-trip

---

### 2.2 Authorization Server Config

**Goal:** Configure Spring Authorization Server with JPA-backed client storage, PKCE, and custom JWT claims.

**File:** `src/main/java/com/jobagent/jobagent/auth/config/AuthorizationServerConfig.java`

**Configuration:**
- Register `jobagent-spa` client: PKCE + authorization_code, redirect `http://localhost:5173/callback`
- Access token TTL: 30 minutes
- Refresh token TTL: 8 hours
- Custom JWT claims: `tenant_id`, `region`, `user_id`
- JWK RSA key pair generation

**Custom JWT Claims (in access_token):**
| Claim | Source | Example |
|-------|--------|---------|
| `sub` | email | `user@example.com` |
| `user_id` | users.id | `a1b2c3d4-...` |
| `tenant_id` | users.tenant_id | `e5f6g7h8-...` |
| `region` | users.region | `EU` |

**Test (Unit):** `AuthorizationServerConfigTest.java`
- [ ] `RegisteredClientRepository` bean created
- [ ] `jobagent-spa` client exists, requires PKCE
- [ ] JWK source generates valid RSA key
- [ ] Token customizer adds `tenant_id` claim

---

### 2.3 Resource Server Config

**Goal:** Secure API endpoints with JWT validation.

**File:** Update `SecurityConfig.java` — remove `anyRequest().permitAll()` TODO

**Security Rules:**
| Path | Auth | Description |
|------|------|-------------|
| `POST /api/v1/auth/register` | Public | Registration |
| `GET /actuator/health` | Public | Health check |
| `GET /` | Public | Welcome |
| All other `/api/**` | JWT Required | Protected |

**Test (Integration):** `SecurityIntegrationTest.java`
- [ ] `POST /api/v1/auth/register` without token → 201 (or 400)
- [ ] `GET /actuator/health` without token → 200
- [ ] `GET /api/v1/cv` without token → 401
- [ ] `GET /api/v1/cv` with valid JWT → 200 (or 404)
- [ ] `GET /api/v1/cv` with expired JWT → 401

---

### 2.4 Login Flow (Token Endpoint)

**Goal:** Enable OAuth 2.1 PKCE flow for SPA login.

**Built-in Endpoints (Spring Authorization Server):**
| Path | Description |
|------|-------------|
| `/oauth2/authorize` | Start PKCE flow |
| `/oauth2/token` | Exchange code for JWT |
| `/oauth2/revoke` | Revoke token |
| `/.well-known/openid-configuration` | OIDC discovery |
| `/oauth2/jwks` | JWK Set |

**File:** `src/main/java/com/jobagent/jobagent/auth/service/JpaUserDetailsService.java`
- Implements `UserDetailsService`
- `loadUserByUsername(email)` → lookup by email_hash, return Spring Security `UserDetails`

**Test (Integration):** `LoginFlowIntegrationTest.java`
- [ ] Register user → full PKCE flow → receive JWT
- [ ] JWT contains `user_id`, `tenant_id`, `region` claims
- [ ] Call protected endpoint with JWT → 200
- [ ] Disabled user → login fails

---

### 2.5 TenantContextFilter — Extract Tenant from JWT

**Goal:** Extract `tenant_id` from JWT and set in ThreadLocal for RLS enforcement.

**File:** Update `TenantContextFilter.java`
- Extract `tenant_id`, `user_id`, `region` from `JwtAuthenticationToken`
- Set in `TenantContext` (ThreadLocal)
- Clear in `finally` block

**Test (Unit):** `TenantContextFilterTest.java`
- [ ] JWT with `tenant_id` → TenantContext populated
- [ ] No authentication → TenantContext empty
- [ ] After filter → TenantContext cleared (no thread leak)

---

## Sprint 3 — CV Upload (file storage only)

### 3.1 CvDetails JPA Entity

**Goal:** Entity for tracking CV uploads and parsed data.

**File:** `src/main/java/com/jobagent/jobagent/cv/model/CvDetails.java`
- Fields: `user` (ManyToOne LAZY), `fileName`, `contentType`, `fileSize`, `s3Key`, `parsedJson` (JSONB), `status` (enum), `active`, `parsedAt`

**File:** `src/main/java/com/jobagent/jobagent/cv/model/CvStatus.java`
```java
public enum CvStatus { UPLOADED, PARSING, PARSED, FAILED }
```

**Test (Unit):** `CvDetailsTest.java`
- [ ] Builder sets defaults: status=UPLOADED, active=true
- [ ] Links to User entity
- [ ] Enum values: UPLOADED, PARSING, PARSED, FAILED

---

### 3.2 CvDetails Repository

**File:** `src/main/java/com/jobagent/jobagent/cv/repository/CvDetailsRepository.java`
- `findByUserIdAndActiveTrue(UUID)` — get current CV
- `findByUserIdOrderByCreatedAtDesc(UUID)` — CV history
- `deactivateAllByUserId(UUID)` — bulk deactivation via `@Modifying @Query`

**Test (Integration):** `CvDetailsRepositoryIntegrationTest.java`
- [ ] Save → id generated
- [ ] `findByUserIdAndActiveTrue` → returns active CV
- [ ] After deactivate → no active CV

---

### 3.3 FileStorageService (MinIO/S3)

**Goal:** Abstract file storage behind interface.

**File:** `src/main/java/com/jobagent/jobagent/cv/service/FileStorageService.java` (interface)
**File:** `src/main/java/com/jobagent/jobagent/cv/service/MinioFileStorageService.java` (implementation)

**Methods:** `upload()`, `download()`, `generatePresignedUrl()`, `delete()`
**S3 Key Format:** `cv/{tenantId}/{userId}/{uuid}.{ext}`

**Test (Unit):** `MinioFileStorageServiceTest.java`
- [ ] Mock `MinioClient`, verify correct method calls

---

### 3.4 CvUploadService

**Goal:** Orchestrate CV file upload with validation.

**File:** `src/main/java/com/jobagent/jobagent/cv/service/CvUploadService.java`
**File:** `src/main/java/com/jobagent/jobagent/cv/dto/CvUploadResponse.java`

**Validation Rules:**
| Rule | Value | Error |
|------|-------|-------|
| Allowed types | `application/pdf`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document` | 400 |
| Max file size | 10 MB | 400 |

**Test (Unit):** `CvUploadServiceTest.java`
- [ ] Upload PDF → success
- [ ] Upload DOCX → success
- [ ] Upload .txt → throws exception
- [ ] File > 10MB → throws exception
- [ ] Previous CV deactivated

---

### 3.5 CV REST Controller

**File:** `src/main/java/com/jobagent/jobagent/cv/controller/CvController.java`

**Endpoints:**
| Method | Path | Response | Description |
|--------|------|----------|-------------|
| POST | `/api/v1/cv` | 201 | Upload new CV (multipart) |
| GET | `/api/v1/cv` | 200 | Get active CV summary |
| GET | `/api/v1/cv/{id}/download` | 200 | Presigned download URL |
| GET | `/api/v1/cv/history` | 200 | All CV versions |
| DELETE | `/api/v1/cv/{id}` | 204 | Soft-delete |

**Test (Integration):** `CvControllerIntegrationTest.java`
- [ ] Upload PDF → 201
- [ ] Upload .txt → 400
- [ ] Get active → 200
- [ ] No active CV → 404
- [ ] Download URL → 200
- [ ] Delete → 204

---

## Sprint 4 — CV AI Parsing (Spring AI + Ollama)

### 4.1 CV Text Extraction Service (Apache Tika)

**File:** `src/main/java/com/jobagent/jobagent/cv/service/CvTextExtractor.java`
- Uses Apache Tika to extract text from PDF/DOCX

**Test (Unit):** `CvTextExtractorTest.java`
- [ ] Extract from PDF → non-empty string
- [ ] Corrupt file → throws `CvParsingException`

---

### 4.2 CvParserAgent (Spring AI ChatClient)

**File:** `src/main/java/com/jobagent/jobagent/cv/service/CvParserAgent.java`
- Uses `ChatClient` + prompt template `prompts/cv-parse.st`
- Input: raw CV text → Output: structured JSON (`CvParsedData` record)
- `@CircuitBreaker` + `@Retry` on AI call

**File:** `src/main/java/com/jobagent/jobagent/cv/dto/CvParsedData.java`
```java
public record CvParsedData(
    String fullName, String currentTitle,
    List<String> skills,
    List<ExperienceEntry> experience,
    List<EducationEntry> education,
    List<String> languages,
    String summary
) {}
```

**Test (Unit):** `CvParserAgentTest.java`
- [ ] Mock ChatClient → returns sample JSON
- [ ] Verify prompt contains CV text
- [ ] LLM failure → fallback triggered

---

### 4.3 CV Embedding Service

**File:** `src/main/java/com/jobagent/jobagent/cv/service/CvEmbeddingService.java`
**File:** `src/main/java/com/jobagent/jobagent/cv/model/CvEmbedding.java` — `@OneToOne(LAZY)` with CvDetails, `vector(1536)` column

**Test (Unit):** `CvEmbeddingServiceTest.java`
- [ ] Mock `EmbeddingModel` → verify embedding saved

---

### 4.4 CV RAG Ingestion Service

**File:** `src/main/java/com/jobagent/jobagent/cv/service/CvRagIngestionService.java`
- Chunk by section: EXPERIENCE, EDUCATION, SKILLS, PROJECTS, SUMMARY
- Create Spring AI `Document` objects with metadata: `{ cv_id, tenant_id, section, doc_type: "cv_chunk" }`
- Store in `PgVectorStore`, remove old chunks on re-upload

**Test (Unit):** `CvRagIngestionServiceTest.java`
- [ ] Mock PgVectorStore → verify `add()` called with metadata
- [ ] Old chunks deleted before new ones added

---

### 4.5 CV Processing Orchestrator

**File:** `src/main/java/com/jobagent/jobagent/cv/service/CvProcessingService.java`
- Pipeline: Load → Extract (Tika) → Parse (AI) → Embed → Ingest (RAG) → Update status
- On failure: status = FAILED, log error

**Test (Unit):** `CvProcessingServiceTest.java`
- [ ] Mock all services → verify call order
- [ ] On parse failure → status FAILED
- [ ] Happy path → status PARSED, parsedJson stored

---

## ✅ Sprint 5 — Job Search Core (DONE)

> **Implementation Date:** March 1, 2026  
> **Report:** `docs/SPRINT-5.0-REPORT.md`

### ✅ 5.1 JobListing JPA Entity (DONE)

**File:** `src/main/java/com/jobagent/jobagent/jobsearch/model/JobListing.java`
- Fields: `title`, `company`, `location`, `description` (TEXT), `salaryMin`, `salaryMax`, `salaryCurrency`, `employmentType` (FULL_TIME/PART_TIME/CONTRACT), `remoteType` (REMOTE/HYBRID/ON_SITE), `sourceUrl`, `externalId`, `expiresAt`, `status`
- Skills stored as JSONB for efficient matching
- Multi-tenant with TenantEntityListener

**Test (Unit):** `JobListingTest.java` — 15 tests ✅

---

### ✅ 5.2 Enums (DONE)

**Files:**
- `JobStatus.java` — ACTIVE, EXPIRED, APPLIED, FILLED, SAVED
- `EmploymentType.java` — FULL_TIME, PART_TIME, CONTRACT, etc.
- `RemoteType.java` — ON_SITE, REMOTE, HYBRID

---

### ✅ 5.3 JobListing Repository (DONE)

**File:** `src/main/java/com/jobagent/jobagent/jobsearch/repository/JobListingRepository.java`
- `findByTenantIdAndStatus(UUID, JobStatus, Pageable)`
- `searchByTitle()`, `fullTextSearch()` via PostgreSQL tsvector
- `findBySkillsContaining()` via JSONB operators
- `advancedSearch()` with multiple criteria
- `findExpiredJobs()` for scheduled expiration

---

### ✅ 5.4 Job DTOs (DONE)

**Files:**
- `JobListingResponse.java` — API response with factory method
- `JobSearchRequest.java` — Search filters with defaults
- `JobMatchScore.java` — Match result with percentage
- `CreateJobRequest.java` — Job creation request

---

### ✅ 5.5 JobSearchService (DONE)

**File:** `src/main/java/com/jobagent/jobagent/jobsearch/service/JobSearchService.java`
- Get active jobs, search with filters, create jobs
- External ID deduplication, status updates
- Mark expired jobs

**Test (Unit):** `JobSearchServiceTest.java` — 8 tests ✅

---

### ✅ 5.6 JobMatchingService (DONE)

**File:** `src/main/java/com/jobagent/jobagent/jobsearch/service/JobMatchingService.java`
- Match jobs to user CV skills
- Calculate match percentage
- Skill matching: exact, partial, alias support

**Test (Unit):** `JobMatchingServiceTest.java` — 10 tests ✅

---

### ✅ 5.7 JobSearchController (DONE)

**File:** `src/main/java/com/jobagent/jobagent/jobsearch/controller/JobSearchController.java`
- `GET /api/v1/jobs` — List jobs
- `POST /api/v1/jobs/search` — Search with filters
- `GET /api/v1/jobs/matches` — CV-matched jobs
- `GET /api/v1/jobs/top-matches` — Top N matches
- `POST /api/v1/jobs` — Create job

---

### ✅ 5.8 Flyway Migration (DONE)

**File:** `V7__create_job_listings.sql`
- job_listings table with GIN index for skills
- Full-text search index
- Composite indexes

---

### 🔮 5.9 Job Scraping (Future Sprint)

The following tasks are deferred to a future sprint:
- JobEmbedding entity for vector similarity
- Job scraper services (Indeed, LinkedIn, Generic)
- Web search integration
- Playwright automation

---

## Sprint 5 — Job Search (scraping + matching) — ORIGINAL PLAN

### 5.1 JobListing JPA Entity

**File:** `src/main/java/com/jobagent/jobagent/jobsearch/model/JobListing.java`
- Fields: `title`, `company`, `location`, `description` (TEXT), `salaryMin`, `salaryMax`, `salaryCurrency`, `employmentType` (FULL_TIME/PART_TIME/CONTRACT), `remoteType` (REMOTE/HYBRID/ON_SITE), `source`, `sourceUrl`, `externalId`, `postedAt`, `expiresAt`, `matchScore`, `matchExplanation`

**Test (Unit):** `JobListingTest.java` — builder, fields, BaseEntity

---

### 5.2 JobEmbedding JPA Entity

**File:** `src/main/java/com/jobagent/jobagent/jobsearch/model/JobEmbedding.java`
- `@OneToOne(LAZY)` with JobListing, `vector(1536)` embedding

**Test (Unit):** `JobEmbeddingTest.java` — creates, links to JobListing

---

### 5.3 JobListing Repository

**File:** `src/main/java/com/jobagent/jobagent/jobsearch/repository/JobListingRepository.java`
- `findByTenantId(UUID, Pageable)`, `findByExternalIdAndSource(String, String)`, `searchByTenantId(UUID, String query, Pageable)` via `@Query`

**Test (Integration):** `JobListingRepositoryIntegrationTest.java`
- [ ] Save, paginate, find by external ID, search by title

---

### 5.4 Job Source Config

**File:** `src/main/java/com/jobagent/jobagent/jobsearch/model/JobSourceConfig.java`
- Fields: `name`, `baseUrl`, `scraperType` (INDEED/LINKEDIN/GLASSDOOR/CUSTOM/WEB_SEARCH), `country`, `enabled`

**File:** `src/main/java/com/jobagent/jobagent/jobsearch/model/ScraperType.java`

**Note:** May require new Flyway migration `V9__create_job_source_configs.sql`

---

### 5.5 Job Scraper Service (JSoup)

**Interface:** `src/main/java/com/jobagent/jobagent/jobsearch/service/JobScraperService.java`
**Implementations:** `IndeedScraper`, `LinkedInScraper`, `GenericScraper` (in `scrapers/` sub-package)
**Factory:** `ScraperFactory.java` — selects scraper by `ScraperType`

**Test (Unit):** `IndeedScraperTest.java` — mock HTML, verify job extraction

---

### 5.6 Job Embedding Service

**File:** `src/main/java/com/jobagent/jobagent/jobsearch/service/JobEmbeddingService.java`
- Generate embeddings via Spring AI `EmbeddingModel`, store in `job_embeddings`

**Test (Unit):** `JobEmbeddingServiceTest.java` — mock EmbeddingModel

---

### 5.7 Job Matching Service (Vector Similarity)

**File:** `src/main/java/com/jobagent/jobagent/jobsearch/service/JobMatchingService.java`
**File:** `src/main/java/com/jobagent/jobagent/jobsearch/dto/JobMatchResult.java`

**Algorithm:**
1. Load CV embedding → compute cosine similarity with each job embedding
2. Rank by score DESC
3. For top-N: RAG retrieval (CV chunks relevant to job) → LLM match explanation
4. Cache results in Redis (1hr TTL)

**Test (Unit):** `JobMatchingServiceTest.java`
- [ ] Mock embeddings → verify ranking order (highest first)
- [ ] No CV embedding → throws exception

---

### 5.8 JobSearch REST Controller

**File:** `src/main/java/com/jobagent/jobagent/jobsearch/controller/JobSearchController.java`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/jobs?q=...&location=...&remote=...&page=&size=` | Search & match |
| GET | `/api/v1/jobs/{id}` | Job detail |
| POST | `/api/v1/jobs/refresh` | Force re-search (bypass cache) |
| POST | `/api/v1/jobs/{id}/save` | Bookmark |
| GET | `/api/v1/jobs/saved` | Saved jobs list |

**Test (Integration):** `JobSearchControllerIntegrationTest.java`

---

## Sprint 6 — Motivation Letter Generation

### 6.1 MotivationLetter JPA Entity

**File:** `src/main/java/com/jobagent/jobagent/motivation/model/MotivationLetter.java`
- FKs to User, CvDetails, JobListing
- Fields: `letterText` (TEXT), `tone` (PROFESSIONAL/CASUAL/CREATIVE), `language` ("en"), `status` (DRAFT/APPROVED/USED/ARCHIVED), `version` (default 1), `pdfS3Key`

**Enums:** `LetterTone.java`, `LetterStatus.java`

**Test (Unit):** `MotivationLetterTest.java` — defaults, relationships

---

### 6.2 MotivationLetter Repository

**File:** `src/main/java/com/jobagent/jobagent/motivation/repository/MotivationLetterRepository.java`
- `findByUserIdOrderByCreatedAtDesc`, `findByJobListingIdAndUserId`

**Test (Integration):** `MotivationLetterRepositoryIntegrationTest.java`

---

### 6.3 MotivationWriterAgent (Spring AI + RAG)

**File:** `src/main/java/com/jobagent/jobagent/motivation/service/MotivationWriterAgent.java`

**RAG Flow:**
1. Query PgVectorStore: job description → top-5 CV chunks (filter: `doc_type=cv_chunk`, `tenant_id`)
2. Augmented Prompt: system rules + CV context (RAG) + job desc + tone + language
3. LLM generates letter grounded in CV data

**Test (Unit):** `MotivationWriterAgentTest.java` — mock ChatClient, mock PgVectorStore

---

### 6.4 PDF Generation Service (OpenPDF)

**File:** `src/main/java/com/jobagent/jobagent/motivation/service/PdfGenerationService.java`
- `generatePdf(String letterText, String userName, String companyName, LocalDate date)` → `byte[]`
- Professional formatting, upload to S3

**Test (Unit):** `PdfGenerationServiceTest.java` — non-empty output, valid PDF header

---

### 6.5 MotivationLetterService (Orchestrator)

**File:** `src/main/java/com/jobagent/jobagent/motivation/service/MotivationLetterService.java`

**Methods:** `generate()`, `regenerate()` (increment version), `update()` (user edits), `approve()`, `getPdfUrl()`

**DTOs:** `GenerateLetterRequest`, `RegenerateRequest`, `UpdateLetterRequest`, `MotivationLetterResponse`

**Test (Unit):** `MotivationLetterServiceTest.java` — mock agent, repo, PDF service

---

### 6.6 Motivation Letter REST Controller

**File:** `src/main/java/com/jobagent/jobagent/motivation/controller/MotivationLetterController.java`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/motivation-letters` | Generate |
| GET | `/api/v1/motivation-letters/{id}` | Get |
| PUT | `/api/v1/motivation-letters/{id}` | Edit text |
| POST | `/api/v1/motivation-letters/{id}/regenerate` | Regenerate |
| POST | `/api/v1/motivation-letters/{id}/approve` | Approve |
| GET | `/api/v1/motivation-letters/{id}/pdf` | Download PDF URL |
| GET | `/api/v1/motivation-letters` | List all |
| DELETE | `/api/v1/motivation-letters/{id}` | Archive |

**Test (Integration):** `MotivationLetterControllerIntegrationTest.java`

---

## Sprint 7 — Job Application

### 7.1 Application + Event + Note Entities

**Files:**
- `application/model/Application.java` — FKs to User, JobListing, MotivationLetter, CvDetails
- `application/model/ApplicationEvent.java` — timeline entries (old/new status)
- `application/model/ApplicationNote.java` — user notes

**Enums:**
- `ApplicationStatus` — PENDING, SUBMITTED, INTERVIEW, OFFERED, REJECTED, WITHDRAWN, FAILED
- `SubmissionMethod` — EMAIL, API, FORM, MANUAL

**Test (Unit):** `ApplicationTest.java` — defaults, FK links, events list

---

### 7.2 Application Repositories

- `ApplicationRepository` — `findByUserIdOrderByCreatedAtDesc`, `existsByJobListingIdAndUserId`, `countByStatus`
- `ApplicationEventRepository` — `findByApplicationIdOrderByCreatedAtAsc`
- `ApplicationNoteRepository`

**Test (Integration):** `ApplicationRepositoryIntegrationTest.java`

---

### 7.3 ApplyAgent (Submission Automation)

**Strategy Pattern:**
```
SubmissionStrategy (interface)
├── EmailSubmitter          — JavaMailSender
├── ApiSubmitter            — RestClient to job board APIs
├── FormSubmitter           — Playwright (stub for now)
└── ManualSubmitter         — No-op, tracking only
```

**File:** `application/service/ApplyAgent.java` — resolves strategy, executes submission

**Test (Unit):** `ApplyAgentTest.java` — mock submitters, verify correct strategy chosen

---

### 7.4 ApplicationService (Orchestrator)

**File:** `application/service/ApplicationService.java`

**Methods:** `apply()`, `retry()`, `withdraw()`

**Kafka Events:**
| Topic | Event |
|-------|-------|
| `application.submitted` | `{ applicationId, userId, tenantId, jobTitle, company, status }` |
| `application.status-changed` | `{ applicationId, oldStatus, newStatus }` |

**Validation:**
- Duplicate check: `existsByJobListingIdAndUserId` → 409
- Letter must be APPROVED status
- After submission: letter status → USED

**Test (Unit):** `ApplicationServiceTest.java` — mock all deps, verify happy/error paths

---

### 7.5 Application REST Controller

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/applications` | Submit application |
| GET | `/api/v1/applications/{id}` | Detail + events |
| POST | `/api/v1/applications/{id}/retry` | Retry failed |
| DELETE | `/api/v1/applications/{id}` | Withdraw |

**Test (Integration):** `ApplicationControllerIntegrationTest.java`

---

## Sprint 8 — Application Dashboard + Statistics

### 8.1 Application Statistics DTO

**File:** `application/dto/ApplicationStats.java`
```java
public record ApplicationStats(long total, long pending, long submitted,
    long interview, long offered, long rejected, long withdrawn, long failed) {}
```

---

### 8.2 Application Dashboard Service

**File:** `application/service/ApplicationDashboardService.java`

**Methods:**
- `listApplications(userId, filter, pageable)` — filtered pagination
- `getDetail(userId, applicationId)` — events timeline + notes
- `getStats(userId)` — aggregate counts by status
- `updateStatus(userId, applicationId, request)` — manual status update + event
- `addNote(userId, applicationId, request)` — persist note

**DTOs:** `ApplicationFilter`, `ApplicationSummary`, `ApplicationDetailResponse`, `UpdateStatusRequest`, `AddNoteRequest`

**Test (Unit):** `ApplicationDashboardServiceTest.java` — stats, status update, add note

---

### 8.3 Dashboard REST Controller

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/applications?status=...&from=...&to=...&sort=...` | Paginated + filtered list |
| GET | `/api/v1/applications/stats` | Aggregated statistics |
| PATCH | `/api/v1/applications/{id}/status` | Manual status update |
| POST | `/api/v1/applications/{id}/notes` | Add note |

**Test (Integration):** `DashboardControllerIntegrationTest.java`

---

## Sprint 9 — Vue.js Frontend Integration

### 9.1 Auth Pages
- Update `LoginView.vue`, `RegisterView.vue`, `auth.ts` store
- Implement PKCE flow, token-in-memory, auto-refresh
- Create `auth.api.ts`
- **Test:** `auth.store.spec.ts`

### 9.2 CV Upload Page
- Update `CvUploadView.vue`, create `cv.store.ts`, `cv.api.ts`
- Drag-and-drop upload, progress bar, parsed summary, history
- **Test:** `cv.store.spec.ts`

### 9.3 Job Search Page
- Update `JobSearchView.vue`, `JobDetailView.vue`, create `jobs.store.ts`, `jobs.api.ts`
- Search bar, filters, results with match scores, detail with explanation, bookmarks
- **Test:** `jobs.store.spec.ts`

### 9.4 Motivation Letter Page
- Update `MotivationListView.vue`, create `MotivationDetailView.vue`, `motivation.store.ts`, `motivation.api.ts`
- Generate, edit, regenerate, approve, download PDF
- **Test:** `motivation.store.spec.ts`

### 9.5 Applications Dashboard Page
- Update `ApplicationsListView.vue`, create `ApplicationDetailView.vue`, `applications.store.ts`, `applications.api.ts`
- Stats cards, status filters, event timeline, notes, status update
- **Test:** `applications.store.spec.ts`

### 9.6 Dashboard Home Page
- Update `DashboardView.vue` — welcome, stats, recent apps, quick actions
- **Test:** `DashboardView.spec.ts`

---

## Sprint 10 — Production Hardening

### 10.1 Re-enable JWT Auth
- Remove `permitAll()` TODO, enforce auth on all `/api/**` except register + health

### 10.2 Integration Tests (Testcontainers)
- `IntegrationTestBase.java` — shared containers (PostgreSQL, Valkey, Kafka, MinIO)
- All `*IntegrationTest.java` use Testcontainers (no Docker Compose needed in CI)

### 10.3 Kafka Event Consumers
- `ApplicationEventConsumer.java` — DLT, notifications

### 10.4 Email Encryption (GDPR Compliance)
- Add `@Convert(converter = EncryptedStringConverter.class)` to `User.email`, `UserProfile.phone`, `UserProfile.address`
- Externalize key via env var / Vault
- Data migration for existing users

### 10.5 Rate Limiting (Resilience4j)
- Auth: 10 req/min/IP, AI: 5 req/min/user, Search: 30 req/min/user

### 10.6 OpenAPI / Swagger Docs
- springdoc-openapi, annotate all controllers, serve at `/swagger-ui.html`

### 10.7 Health Checks & Readiness Probes
- Custom health indicators: PostgreSQL, Valkey, Kafka, MinIO, Ollama

---

## Current Status

| Sprint | Status | Description |
|--------|--------|-------------|
| 0      | ✅ Done | Foundation — Docker, Flyway, common module |
| 1.1    | ✅ Done | User Entity + 11 unit tests |
| 1.2    | ✅ Done | UserProfile Entity + 9 unit tests |
| 1.3    | ✅ Done | UserRepository + UserProfileRepository |
| 1.4    | ✅ Done | RegisterRequest/Response DTOs + 10 validation tests |
| 1.5    | ✅ Done | UserService + RegionResolver + 36 tests |
| 1.6    | ✅ Done | Registration REST Controller + 8 integration tests |
| 2      | ✅ Done | Spring Authorization Server (JWT) |
| 3      | ✅ Done | CV Upload (file storage) |
| 4      | ✅ Done | CV AI Parsing (Spring AI + Ollama) |
| 5      | ✅ Done | Job Search (scraping + matching) |
| 6      | ✅ Done | Motivation Letter Generation |
| 7      | ✅ Done | Job Application Automation |
| 8      | ✅ Done | Application Dashboard + Stats |
| 9      | ✅ Done | Vue.js Frontend Integration |
| 10     | ✅ Done | Production Hardening (Swagger, Health, Rate Limiting) |
| 11     | 🟡 WIP | E2E Testing (Playwright) — tests created, verification in progress |

---

## Task & Test Count Summary

| Sprint | Sub-Tasks | Unit Tests | Integration Tests | Total Tests |
|--------|-----------|------------|-------------------|-------------|
| 0      | Done      | —          | —                 | —           |
| 1.1    | Done      | 11 ✅       | 8 (Docker)        | 19          |
| 1.2    | 4         | ~7         | ~4                | ~11         |
| 1.3    | 2         | —          | ~5                | ~5          |
| 1.4    | 2         | ~9         | —                 | ~9          |
| 1.5    | 5         | ~14        | —                 | ~14         |
| 1.6    | 2         | —          | ~7                | ~7          |
| 2.1    | 1         | ~2         | —                 | ~2          |
| 2.2    | 5         | ~4         | —                 | ~4          |
| 2.3    | 5         | —          | ~5                | ~5          |
| 2.4    | 4         | —          | ~4                | ~4          |
| 2.5    | 3         | ~4         | —                 | ~4          |
| 3.1    | 3         | ~4         | —                 | ~4          |
| 3.2    | 3         | —          | ~3                | ~3          |
| 3.3    | 4         | ~4         | —                 | ~4          |
| 3.4    | 7         | ~6         | —                 | ~6          |
| 3.5    | 5         | —          | ~7                | ~7          |
| 4.1    | 2         | ~3         | —                 | ~3          |
| 4.2    | 5         | ~4         | —                 | ~4          |
| 4.3    | 3         | ~3         | —                 | ~3          |
| 4.4    | 4         | ~4         | —                 | ~4          |
| 4.5    | 4         | ~5         | —                 | ~5          |
| 5.1-8  | 20        | ~12        | ~4                | ~16         |
| 6.1-6  | 18        | ~12        | ~4                | ~16         |
| 7.1-5  | 15        | ~10        | ~4                | ~14         |
| 8.1-3  | 8         | ~4         | ~4                | ~8          |
| 9.1-6  | 24        | ~12 (Vitest)| —               | ~12         |
| 10.1-7 | 14        | ~6         | ~6                | ~12         |
| **Total** | **~172** | **~138** | **~70**          | **~208**    |

---

## File Creation Summary (All Sprints)

### Sprint 1 — Auth Module Files
```
auth/model/User.java                    ← ✅ 1.1
auth/model/UserProfile.java             ← 1.2
auth/repository/UserRepository.java     ← 1.3
auth/repository/UserProfileRepository.java ← 1.3
auth/dto/RegisterRequest.java           ← 1.4
auth/dto/RegisterResponse.java          ← 1.4
auth/service/UserService.java           ← 1.5
auth/service/RegionResolver.java        ← 1.5
auth/controller/AuthController.java     ← 1.6
```

### Sprint 2 — Auth Server Files
```
auth/model/OAuth2RegisteredClient.java  ← 2.1
auth/config/AuthorizationServerConfig.java ← 2.2
auth/service/JpaUserDetailsService.java ← 2.4
```

### Sprint 3 — CV Upload Files
```
cv/model/CvDetails.java                ← 3.1
cv/model/CvStatus.java                 ← 3.1
cv/repository/CvDetailsRepository.java ← 3.2
cv/service/FileStorageService.java      ← 3.3
cv/service/MinioFileStorageService.java ← 3.3
cv/service/CvUploadService.java         ← 3.4
cv/dto/CvUploadResponse.java           ← 3.4
cv/controller/CvController.java        ← 3.5
```

### Sprint 4 — CV AI Parsing Files
```
cv/service/CvTextExtractor.java        ← 4.1
cv/service/CvParserAgent.java          ← 4.2
cv/dto/CvParsedData.java               ← 4.2
cv/model/CvEmbedding.java              ← 4.3
cv/service/CvEmbeddingService.java     ← 4.3
cv/service/CvRagIngestionService.java  ← 4.4
cv/service/CvProcessingService.java    ← 4.5
```

### Sprint 5 — Job Search Files
```
jobsearch/model/JobListing.java        ← 5.1
jobsearch/model/JobEmbedding.java      ← 5.2
jobsearch/model/JobSourceConfig.java   ← 5.4
jobsearch/model/ScraperType.java       ← 5.4
jobsearch/repository/JobListingRepository.java ← 5.3
jobsearch/repository/JobEmbeddingRepository.java ← 5.6
jobsearch/service/JobScraperService.java ← 5.5
jobsearch/service/scrapers/*           ← 5.5
jobsearch/service/ScraperFactory.java  ← 5.5
jobsearch/service/JobEmbeddingService.java ← 5.6
jobsearch/service/JobMatchingService.java ← 5.7
jobsearch/dto/JobMatchResult.java      ← 5.7
jobsearch/controller/JobSearchController.java ← 5.8
```

### Sprint 6 — Motivation Letter Files
```
motivation/model/MotivationLetter.java ← 6.1
motivation/model/LetterTone.java       ← 6.1
motivation/model/LetterStatus.java     ← 6.1
motivation/repository/MotivationLetterRepository.java ← 6.2
motivation/service/MotivationWriterAgent.java ← 6.3
motivation/service/PdfGenerationService.java ← 6.4
motivation/service/MotivationLetterService.java ← 6.5
motivation/dto/*                        ← 6.5
motivation/controller/MotivationLetterController.java ← 6.6
```

### Sprint 7 — Application Module Files
```
application/model/Application.java     ← 7.1
application/model/ApplicationEvent.java ← 7.1
application/model/ApplicationNote.java ← 7.1
application/model/ApplicationStatus.java ← 7.1
application/model/SubmissionMethod.java ← 7.1
application/repository/*              ← 7.2
application/service/ApplyAgent.java    ← 7.3
application/service/SubmissionStrategy.java ← 7.3
application/service/submitters/*       ← 7.3
application/service/ApplicationService.java ← 7.4
application/dto/*                      ← 7.4
application/controller/ApplicationController.java ← 7.5
```

### Sprint 8 — Dashboard Extension Files
```
application/dto/ApplicationStats.java  ← 8.1
application/dto/ApplicationFilter.java ← 8.2
application/service/ApplicationDashboardService.java ← 8.2
```

### Sprint 9 — Vue.js Frontend Files
```
jobagent-ui/src/api/{auth,cv,jobs,motivation,applications}.api.ts
jobagent-ui/src/stores/{cv,jobs,motivation,applications}.store.ts
jobagent-ui/src/views/* (update existing stubs)
```

### Sprint 10 — Production Hardening Files
```
common/config/RateLimitConfig.java     ← 10.5
common/config/OpenApiConfig.java       ← 10.6
application/event/ApplicationEventConsumer.java ← 10.3
IntegrationTestBase.java               ← 10.2
```
