# Sprint Backlog — Implementation Plan

> **Philosophy:** Small, testable tasks. Each sprint delivers one working piece.  
> **Rule:** Implement → Test → Fix → Commit → Next.

---

## ✅ Sprint 0 — Foundation (DONE)
- Architecture docs, Docker Compose, common module, Flyway V1-V8, package stubs
- **Verified:** App starts, health UP, 18 tables, 13 RLS policies

---

## Sprint 1 — User Entity + Registration Endpoint (no auth yet)

### 1.1 User JPA Entity
- Create `User.java` entity in `auth` package
- Map to `users` table, use `BaseEntity`, `@EntityListeners`
- Store `email` as plaintext + `email_hash` (SHA-256) for lookups
- **Note:** Email encryption postponed to Sprint 10.4 (production hardening)
- **Test:** Unit test — entity creates, tenant_id auto-set

### 1.2 UserProfile JPA Entity
- Create `UserProfile.java` entity in `auth` package
- Map to `user_profiles` table, `@OneToOne` with User
- **Test:** Unit test — profile links to user

### 1.3 User Repository
- Create `UserRepository` (Spring Data JPA)
- `findByEmailHash()`, `existsByEmailHash()`
- **Test:** Verify interface compiles

### 1.4 Registration DTO + Service
- `RegisterRequest` (email, password, fullName, country)
- `UserService.register()` — hash email, encrypt email, BCrypt password, save
- **Test:** Unit test with mocked repo

### 1.5 Registration REST Controller
- `POST /api/v1/auth/register` → returns 201 + userId
- Validation (`@Valid`, `@Email`, `@NotBlank`)
- `DuplicateResourceException` if email exists
- **Test:** curl `POST /api/v1/auth/register` → 201, duplicate → 409

---

## Sprint 2 — Spring Authorization Server (JWT auth)

### 2.1 Auth Server Config
- Configure `RegisteredClient` for SPA (PKCE)
- JWK key pair generation
- JWT token customizer (tenant_id, region claims)

### 2.2 Resource Server Config
- Re-enable `oauth2ResourceServer().jwt()`
- Remove `permitAll()`, restore endpoint protection
- Keep `/api/v1/auth/register`, `/actuator/health` public

### 2.3 Login Flow
- `POST /oauth2/token` with PKCE → JWT
- **Test:** Register → Login → Get JWT → Call protected endpoint

---

## Sprint 3 — CV Upload (file storage only)

### 3.1 CvDetails JPA Entity + Repository
### 3.2 MinIO File Upload Service
### 3.3 `POST /api/v1/cv/upload` Controller
### 3.4 `GET /api/v1/cv/{id}/download` (presigned URL)
- **Test:** Upload PDF → stored in MinIO → download URL works

---

## Sprint 4 — CV AI Parsing (Spring AI + Ollama)

### 4.1 CV Text Extraction (Apache Tika)
### 4.2 CV AI Parser (Spring AI ChatClient + prompt template)
### 4.3 CV Embedding Generation (EmbeddingModel → pgvector)
### 4.4 RAG Ingestion (PgVectorStore chunks)
- **Test:** Upload CV → parsed JSON → embeddings stored

---

## Sprint 5 — Job Search (scraping + matching)

### 5.1 JobListing Entity + Repository
### 5.2 Job Source Config Entity
### 5.3 Job Scraper Service (JSoup)
### 5.4 Job-CV Matching (vector similarity)
- **Test:** Scrape jobs → match score against CV

---

## Sprint 6 — Motivation Letter Generation

### 6.1 MotivationLetter Entity + Repository
### 6.2 Motivation Writer Agent (Spring AI + RAG context)
### 6.3 PDF Generation (OpenPDF)
### 6.4 `POST /api/v1/motivation-letters/generate`
- **Test:** Generate letter for a job+CV → PDF stored

---

## Sprint 7 — Job Application

### 7.1 Application Entity + Events + Repository
### 7.2 Apply Agent (email/API submission)
### 7.3 Kafka events (application.submitted)
### 7.4 `POST /api/v1/applications`
- **Test:** Apply to job → status tracked → event published

---

## Sprint 8 — Application Dashboard

### 8.1 Dashboard statistics queries
### 8.2 `GET /api/v1/applications` (list + filters)
### 8.3 `GET /api/v1/applications/stats`
- **Test:** View applications, stats aggregation

---

## Sprint 9 — Vue.js Frontend Integration

### 9.1 Login/Register pages (connect to backend)
### 9.2 CV upload page
### 9.3 Job search page
### 9.4 Motivation letter page
### 9.5 Applications dashboard page
- **Test:** Full UI flow end-to-end

---

## Sprint 10 — Production Hardening

### 10.1 Re-enable JWT auth (remove permitAll TODO)
### 10.2 Integration tests (Testcontainers)
### 10.3 Kafka event consumers
### 10.4 Email Encryption (GDPR compliance)
- Add `EncryptedStringConverter` for email field
- Migrate existing data: `UPDATE users SET email = pgp_sym_encrypt(email, key)`
- Add secrets management (AWS Secrets Manager/Vault)
- **Test:** Verify encryption/decryption, bulk operations performance
### 10.5 Rate limiting (Resilience4j)
### 10.6 OpenAPI/Swagger docs

---

## Current Status

| Sprint | Status | Next Task |
|--------|--------|-----------|
| 0      | ✅ Done | — |
| 1.1    | ✅ Done | User Entity complete |
| 1.2    | 🔵 Next | UserProfile Entity |
| 1.3-10 | ⬜ Planned | — |
