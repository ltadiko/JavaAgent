# Chat History — Architecture Design Session

**Date:** 2026-02-19  
**Participants:** Developer + GitHub Copilot  
**Topic:** JobAgent — Global AI Job Application Platform — Architecture & Design

---

## Session 1: Initial Architecture Request

### User

> I want to build a global job agent AI application which is scalable, reliable, maintainable, deployable to cloud, and testable locally using Docker & Docker Compose using Java 25 and Spring Boot 4, Spring AI, PostgreSQL.
>
> The application aims to help job seekers find the right job positions by looking at configured websites or general web search. Finding the right jobs depends on the profile, helping customers with writing motivations for the matched jobs and sending the application automatically.
>
> It should be like a multi-tenant cloud application — it can scale up and should work for worldwide customers. I want to keep the data of customers in their corresponding country to fulfill country-specific regulations.
>
> I want to start use case by use case. Can you start with writing design MD files, architecture by use case?

### Copilot — Action Taken

Created the following architecture documents under `docs/architecture/`:

| Document | Content Summary |
|----------|-----------------|
| `00-SYSTEM-ARCHITECTURE.md` | Vision, quality attributes, tech stack, high-level component diagram, multi-tenancy & data residency (database-per-region + RLS), personal data & CV storage strategy, package structure, Docker Compose setup, Kubernetes deployment |
| `01-UC-REGISTER-LOGIN.md` | Email/password + OAuth 2.1 social login, region determination from country, JWT tokens, API endpoints with request/response examples, security considerations |
| `02-UC-UPLOAD-CV.md` | Multipart upload, CVAnalyzerAgent (Spring AI) for text extraction (Tika) + AI parsing + pgvector embedding, S3 storage with encryption, prompt templates |
| `03-UC-SEARCH-JOBS.md` | JobFinderAgent with tool calling (LinkedIn, Indeed, Glassdoor, web search), semantic ranking via cosine similarity, caching in Redis, configurable job sources, rate limiting |
| `04-UC-GENERATE-MOTIVATION.md` | MotivationWriterAgent with structured prompts (CV + job description + tone + language), PDF generation, versioned regeneration with history, multi-language support |
| `05-UC-APPLY-JOB.md` | ApplyAgent with 3 submission methods (email/API/web form via Playwright), async processing via Kafka, idempotency & duplicate prevention, event tracking |
| `06-UC-VIEW-APPLICATIONS.md` | Dashboard with Kanban/list/analytics views, status management with allowed transitions, application timeline, notes, statistics API, notifications via Kafka events |
| `07-DATA-MODEL.md` | Complete DDL for all tables, ER diagram, pgvector indexes, Row-Level Security policies, AES-256 encryption strategy with JPA converters, Flyway migrations, backup & recovery |

---

## Session 2: Personal Data & CV Storage Question

### User

> How are we storing personal information & CV details?

### Copilot — Response

Explained the storage strategy already documented in `00-SYSTEM-ARCHITECTURE.md` §6:

- **User credentials** → PostgreSQL `users` table (password: bcrypt; email: AES-256 column encryption)
- **Profile metadata** → PostgreSQL `user_profiles` (AES-256 column encryption for PII fields)
- **Raw CV file (PDF/DOCX)** → S3-compatible object store (SSE-S3 server-side encryption)
- **Parsed CV (structured)** → PostgreSQL `cv_details` (AES-256 column encryption for sensitive fields)
- **CV embeddings (vector)** → PostgreSQL pgvector extension (not PII — derived feature vectors)

Key security measures:
- Column-level encryption using Spring `@Convert` with `AttributeConverter` delegating to AES-256-GCM
- Encryption keys stored in cloud KMS (never in database)
- S3 access via short-lived pre-signed URLs (5-minute TTL)
- Tenant isolation via `tenant_id` + PostgreSQL Row-Level Security
- GDPR right-to-erasure via dedicated `DELETE /api/v1/users/{id}/data` endpoint

---

## Session 3: IDE Restart & Architecture Finalization

### User

> I had a forced restart of my IntelliJ IDEA. Can you continue with finalizing the architecture for all use cases?

### Copilot — Action Taken

All 8 architecture files were found empty (lost during IDE restart). Rewrote all documents from scratch with comprehensive content including:
- Sequence diagrams for every use case
- Complete API endpoint definitions with request/response examples
- Spring AI agent designs with code snippets and prompt templates
- Data model DDL for each use case
- Error handling tables
- Testing strategies per use case
- Security & privacy considerations
- Caching strategies, rate limiting, async processing patterns

---

## Session 4: Auth Server Decision — Embedded Spring Authorization Server

### User

> I would like to choose embedded Spring Authorization Server.

### Copilot — Action Taken

Updated 3 documents to reflect this decision:

#### `00-SYSTEM-ARCHITECTURE.md` changes:
- Tech stack row: `Embedded Spring Authorization Server (OAuth 2.1 / OpenID Connect)` — removed Keycloak reference
- Package structure: `auth/` module expanded with `config/` and `federation/` sub-packages

#### `01-UC-REGISTER-LOGIN.md` changes:
- §5.3 Social Login: Rewritten with full sequence diagram showing embedded Spring Auth Server as OIDC provider with federated identity to Google/LinkedIn
- §6 API Endpoints: Split into Application endpoints + Spring Auth Server auto-configured endpoints (OIDC discovery, JWKS, authorize, token, revoke, userinfo)
- §10: Converted from "Open Questions" to **Decisions** table (both resolved)
- §11 added: **Spring Authorization Server — Architecture** including:
  - Comparison table: why embedded vs Keycloak (deployment, customisation, data residency, scalability, maintenance, library fit, footprint)
  - Module/package structure with all classes
  - `AuthorizationServerConfig` Java code (dual filter chains, JWK source, server settings)
  - `TokenCustomizer` adding `tenant_id`, `region`, `country`, `user_id` claims to JWTs
  - `FederatedIdentityConfig` with Google + LinkedIn client registrations
  - OAuth2 database schema (3 tables)
  - Token lifecycle table (access 15min, refresh 7d, auth code 5min, ID token 15min)
  - Per-region issuer URLs for cross-region token isolation

#### `07-DATA-MODEL.md` changes:
- ER diagram: Extended with 3 OAuth2 tables
- §3.7 added: Full DDL for `oauth2_registered_client`, `oauth2_authorization`, `oauth2_authorization_consent`
- Flyway migrations: Updated list with `V2__create_oauth2_auth_server_tables.sql`

---

## Key Architectural Decisions Log

| # | Decision | Rationale | Date |
|---|----------|-----------|------|
| ADR-001 | **Modular monolith** (not microservices) | Simpler to start; clear package boundaries allow future extraction | 2026-02-19 |
| ADR-002 | **Database-per-region** + RLS for tenant isolation | Data residency compliance (GDPR, CCPA, PDPA); tenant_id column + Row-Level Security within each region | 2026-02-19 |
| ADR-003 | **AES-256-GCM column-level encryption** for PII | Encrypt at application layer; keys in cloud KMS | 2026-02-19 |
| ADR-004 | **pgvector** for CV/job embeddings | Stays in PostgreSQL (no separate vector DB); IVFFlat indexes for performance | 2026-02-19 |
| ADR-005 | **Embedded Spring Authorization Server** (not Keycloak) | Single JVM, full Java customisation, same regional DB, native Spring Boot 4 integration, zero extra memory overhead | 2026-02-19 |
| ADR-006 | **Spring AI with function calling** for all agents | CVAnalyzerAgent, JobFinderAgent, MotivationWriterAgent, ApplyAgent all use Spring AI ChatClient with tool functions | 2026-02-19 |
| ADR-007 | **Kafka** for async events | Application submission, status changes, follow-up reminders, audit trail | 2026-02-19 |
| ADR-008 | **S3-compatible object store** for files | CV PDFs, motivation letter PDFs; region-local buckets; pre-signed URLs | 2026-02-19 |
| ADR-009 | **MFA deferred to v2** | Focus on core auth flows first | 2026-02-19 |
| ADR-010 | **Flyway** for database migrations | Version-controlled SQL migrations | 2026-02-19 |
| ADR-011 | **Ollama for local AI**, OpenAI/Anthropic for production | Zero-cost local dev; no API keys needed; profile-based switching via Spring AI abstraction | 2026-02-19 |
| ADR-012 | **Vue.js 3 SPA** for frontend | TypeScript, Vite, PrimeVue, Tailwind CSS; OAuth 2.1 PKCE auth; separate deployable (Nginx); communicates via REST API only | 2026-02-19 |
| ADR-013 | **RAG with PgVectorStore included in v1** | pgvector as both vector DB and RAG store via Spring AI `PgVectorStore`. CV chunks ingested during UC-02, RAG retrieval powers UC-03 match explanations and UC-04 motivation letter generation. No separate vector DB needed. | 2026-02-19 |

---

## Documents Produced

```
docs/architecture/
├── 00-SYSTEM-ARCHITECTURE.md          ← System overview, tech stack, multi-tenancy, data storage
├── 01-UC-REGISTER-LOGIN.md            ← UC-01: Register / Login + Spring Auth Server
├── 02-UC-UPLOAD-CV.md                 ← UC-02: Upload & Analyse CV
├── 03-UC-SEARCH-JOBS.md               ← UC-03: Search Jobs
├── 04-UC-GENERATE-MOTIVATION.md       ← UC-04: Generate Motivation Letter
├── 05-UC-APPLY-JOB.md                 ← UC-05: Apply to Job
├── 06-UC-VIEW-APPLICATIONS.md         ← UC-06: View Applications
├── 07-DATA-MODEL.md                   ← Complete data model (DDL, RLS, encryption, Flyway)
├── 08-UC-UI-MODULE.md                 ← UI Module: Vue.js SPA architecture
└── 09-DESIGN-REVIEW-RAG-ANALYSIS.md   ← Pre-implementation review + RAG analysis
```

---

## Session 5: Implementation Order Decision

### User

> I would like to start implementation use case wise. Which would you recommend to start?

### Copilot — Recommendation

Recommended starting with **UC-01: Register / Login** because every other use case depends on an authenticated user. The dependency chain is:

```
UC-01 Register/Login → UC-02 Upload CV → UC-03 Search Jobs → UC-04 Generate Motivation → UC-05 Apply → UC-06 View Applications
```

Proposed implementation plan:

| Sprint | Scope | Deliverables |
|--------|-------|--------------|
| **Sprint 0** | Foundation (infrastructure + common) | `docker-compose.yml`, `pom.xml` dependencies, Flyway V1+V2 migrations, common module (encryption, tenant context, base entity), application properties, **Vue.js project scaffolding** (`jobagent-ui/`) |
| **Sprint 1** | UC-01: Register / Login | Spring Authorization Server config, registration + login endpoints, federated identity (Google/LinkedIn), token customizer, tests, **Vue.js: Login/Register views, auth store, PKCE flow** |
| **Sprint 2** | UC-02: Upload CV | CVAnalyzerAgent, S3 integration, Tika text extraction, pgvector embeddings, tests, **Vue.js: CV Upload/Detail views** |
| **Sprint 3** | UC-03: Search Jobs | JobFinderAgent, tool functions for job boards, ranking algorithm, Redis caching, tests, **Vue.js: Job Search/Detail/Saved views** |
| **Sprint 4** | UC-04: Generate Motivation Letter | MotivationWriterAgent, PDF generation, versioning, tests, **Vue.js: Letter Generate/Detail views** |
| **Sprint 5** | UC-05: Apply to Job | ApplyAgent, email/API/form submission, Kafka async processing, tests, **Vue.js: Apply flow integration** |
| **Sprint 6** | UC-06: View Applications | Dashboard APIs, status management, statistics, notifications, tests, **Vue.js: Applications List/Detail/Stats/Kanban views, Dashboard** |

---

## Next Steps

1. **Implement Sprint 0** — Foundation: Docker Compose (with Ollama), pom.xml, Flyway migrations, common module, Vue.js project scaffolding
2. **Implement Sprint 1** — UC-01: Register / Login (backend + frontend)
3. Continue sprint-by-sprint through UC-02 → UC-06

---

## Session 6: Ollama for Local AI

### User

> I want to include lightweight open-source chat generative AI like Ollama for local.

### Copilot — Action Taken

Updated 6 architecture documents to integrate **Ollama** as the local AI provider:

#### `00-SYSTEM-ARCHITECTURE.md` changes:
- Tech stack table: Added `AI Provider (local)` → Ollama and `AI Provider (cloud)` → OpenAI / Anthropic
- Quality attributes: Updated Testability to reference Ollama instead of "mock AI"
- Component diagram: Updated to show `Local: Ollama` and `Cloud: OpenAI / Anthropic`
- Docker Compose: Added `ollama` service (image: `ollama/ollama:latest`, port 11434, persistent volume)
- App container: Changed from `SPRING_AI_OPENAI_API_KEY` to Ollama config (`SPRING_AI_OLLAMA_BASE_URL`, `SPRING_AI_OLLAMA_CHAT_MODEL: mistral`, `SPRING_AI_OLLAMA_EMBEDDING_MODEL: nomic-embed-text`)
- §8.1 added: **AI Provider Strategy** — profile-based switching between Ollama (local) and OpenAI (prod), `application-local.yml` / `application-prod.yml` examples, model recommendations table, embedding dimension note

#### Testing strategy updates (all UC docs):
- `02-UC-UPLOAD-CV.md` — Added "AI Integration: Testcontainers Ollama" row
- `03-UC-SEARCH-JOBS.md` — Added "AI Integration: Testcontainers Ollama" row
- `04-UC-GENERATE-MOTIVATION.md` — Added "AI Integration: Testcontainers Ollama" row
- `05-UC-APPLY-JOB.md` — Added "AI Integration: Testcontainers Ollama" row

#### Key design principle:
All agents (`CVAnalyzerAgent`, `JobFinderAgent`, `MotivationWriterAgent`, `ApplyAgent`) inject Spring AI's `ChatClient` abstraction — they **never reference a specific provider**. Switching from Ollama to OpenAI is purely a configuration change via Spring profiles.

---

## Session 7: Vue.js UI Module

### User

> I would like to include a module for UI as well using Vue.js.

### Copilot — Action Taken

Created `08-UC-UI-MODULE.md` and updated `00-SYSTEM-ARCHITECTURE.md`:

#### New document: `08-UC-UI-MODULE.md`
Comprehensive Vue.js SPA architecture covering:
- **Tech stack**: Vue.js 3 (Composition API, `<script setup>`), TypeScript 5, Vite 6, PrimeVue 4, Tailwind CSS 4, Pinia, Vue Router, Axios, Chart.js, Vue I18n, VeeValidate + Zod
- **Project structure**: Full directory tree — views, components, stores, API clients, composables, types, i18n
- **Routing**: All routes mapped to use cases with lazy loading
- **Authentication**: OAuth 2.1 Authorization Code + PKCE flow (SPA as public client); Pinia auth store with in-memory token storage; Axios interceptor for auto-refresh
- **Page wireframes**: Dashboard, Job Search, Applications Kanban (ASCII)
- **OAuth2 client registration**: `jobagent-spa` as public client with PKCE enforcement
- **CORS configuration**: Spring Boot config for cross-origin SPA requests
- **Docker setup**: Multi-stage Dockerfile (Node build → Nginx Alpine), `nginx.conf` with SPA fallback + API proxy + security headers
- **View ↔ Use Case mapping**: Every Vue view mapped to backend API endpoints
- **Responsive design**: Breakpoints for mobile/tablet/desktop
- **Accessibility**: WCAG 2.1 AA via PrimeVue
- **Testing**: Vitest (unit/component), MSW (integration), Playwright (E2E)
- **CI/CD pipeline**: lint → type-check → test → build → Docker → Helm
- **Performance**: Code splitting, tree shaking, content-hashed caching, < 200 KB gzipped

#### `00-SYSTEM-ARCHITECTURE.md` changes:
- Tech stack: Added `Frontend` row → Vue.js 3, TypeScript, Vite, PrimeVue, Tailwind
- Component diagram: Added Vue.js SPA layer above API Gateway
- Docker Compose: Added `jobagent-ui` service
- Kubernetes: Added `jobagent-ui` deployment + separate Ingress (`app-eu.jobagent.com`)
- Use Case Index: Added entry 8 → `08-UC-UI-MODULE.md`

#### Sprint plan updated:
Every sprint now includes matching frontend work alongside backend work.

---

## Session 8: Implementation Readiness & RAG Analysis

### User

> Are we good to start with implementation? Are there any other points we need to take care at design level? Does it add value if we introduce RAG to our use case or will it be overkill?

### Copilot — Analysis & Recommendation

Created `09-DESIGN-REVIEW-RAG-ANALYSIS.md` covering implementation readiness and RAG analysis. Initially recommended deferring RAG to v2.

### User (follow-up)

> Can you help me choosing the RAG for our use case? I prefer to use open source RAG or vector database.

### Copilot — Recommendation

Recommended **pgvector (PgVectorStore)** over separate vector DBs (Chroma, Qdrant, Milvus) because:
- Zero extra infrastructure (same Postgres)
- Transactional consistency with app data
- Native multi-tenancy via SQL WHERE on tenant_id
- Spring AI first-class `PgVectorStore` support
- HNSW indexing (pgvector 0.7+)
- ~10M vector scale ceiling (sufficient for the use case)

Decision: **RAG with PgVectorStore included in v1.** Updated ADR-013.

---

## Session 9: RAG Integration into Use Case Diagrams

### User

> We have introduced RAG (vector database) to our design. Can you revisit our initial use cases and update sequence diagrams at applicable places?

### Copilot — Action Taken

Updated 6 architecture documents to integrate RAG with `PgVectorStore` into the use case designs:

#### `02-UC-UPLOAD-CV.md` changes:
- Summary: Added CV chunking and PgVectorStore ingestion
- Postconditions: Added "CV chunks ingested into PgVectorStore with metadata" and "old chunks removed on re-upload"
- **Sequence diagram rewritten**: Added steps 6 (chunk CV by section), 7 (ingest chunks into PgVectorStore with metadata), 8 (persist). PostgreSQL column now shows "PostgreSQL / PgVectorStore"
- CVAnalyzerAgent code: Added `VectorStore` dependency, `ingestForRag()` method with section-based chunking (SKILLS, EXPERIENCE, EDUCATION, SUMMARY), metadata (`tenant_id`, `cv_id`, `section`, `doc_type=cv_chunk`), old chunk cleanup on re-upload
- Data model: Split into §8.1 (application tables) and §8.2 (PgVectorStore auto-managed `vector_store` table with HNSW index + metadata GIN index). Added §8.3 dual storage rationale table

#### `03-UC-SEARCH-JOBS.md` changes:
- Summary: Added RAG-powered match explanations
- **Sequence diagram rewritten**: Added steps 4 (RAG: retrieve relevant CV chunks from PgVectorStore for top-N results) and 5 (generate match explanations via LLM with job desc + CV chunks → matchedSkills, missingSkills, reasoning). PostgreSQL column now shows "PostgreSQL / PgVectorStore"
- JobFinderAgent code: Added `VectorStore` dependency, `enrichWithMatchExplanations()` method with filtered similarity search (`tenant_id`, `doc_type=cv_chunk`, top-5, threshold 0.7), LLM-generated `MatchExplanation`
- Search response: `matchedSkills`/`missingSkills` moved into a richer `matchExplanation` object with `reasoning` field

#### `04-UC-GENERATE-MOTIVATION.md` changes (biggest impact):
- Summary: Rewritten to describe RAG-powered generation via PgVectorStore
- **Sequence diagram rewritten**: Shows `QuestionAnswerAdvisor` flow — step 3a (RAG retrieval: query vector store with job description), 3b (augmented prompt: system rules + CV chunks + user query), 3c (LLM generates grounded letter). Added `PgVectorStore` as separate column
- MotivationWriterAgent code: Completely rewritten with `QuestionAnswerAdvisor`, filtered search (`tenant_id`, `doc_type=cv_chunk`, top-5, threshold 0.65), RAG context template
- Added §7.2: "How RAG Improves Letter Quality" comparison table (with vs without RAG)
- Added §7.3: "RAG Flow Detail" — step-by-step walkthrough with example retrieved chunks and augmented prompt

#### `00-SYSTEM-ARCHITECTURE.md` changes:
- CV processing pipeline: Added steps 5 (chunk by section), 6 (ingest into PgVectorStore)
- Package structure: Added `rag/` package under `common/` module

#### `07-DATA-MODEL.md` changes:
- Added §3.3.1: PgVectorStore table DDL with HNSW index + metadata GIN index
- Added metadata schema documentation (`cv_id`, `tenant_id`, `section`, `doc_type`)
- Added `doc_type` values table (cv_chunk now, company_knowledge in v2)
- Added dual storage rationale table
- Flyway: Added `V4__create_pgvector_store_rag.sql` migration

#### `09-DESIGN-REVIEW-RAG-ANALYSIS.md` changes:
- Completely rewritten from "RAG deferred to v2" to "RAG is part of v1"
- §2.3: How RAG is used per use case (UC-02 ingestion, UC-03 retrieval, UC-04 full pipeline)
- §2.4: Vector store YAML configuration (local Ollama + prod OpenAI)
- §2.5: pgvector vs separate vector DB comparison
- §2.6: v2 RAG enhancements (company knowledge, past letters, job Q&A)

#### Additional `07-DATA-MODEL.md` changes (continuation):
- ER diagram: Added `vector_store (RAG)` table with content, metadata (cv_id, tenant_id, section, doc_type), and embedding columns
- §4 RLS: Added note that `vector_store` does NOT use PostgreSQL RLS; tenant isolation enforced via `filterExpression` in `SearchRequest` → `WHERE metadata->>'tenant_id' = ?`
- §8 Performance: Added RAG retrieval quality tuning (topK, similarityThreshold) and JSONB metadata filtering notes

#### Additional `00-SYSTEM-ARCHITECTURE.md` changes (continuation):
- Tech stack: Added `Vector Store / RAG` row → pgvector + Spring AI PgVectorStore (HNSW index, RAG retrieval)
- §6.5 GDPR Right-to-Erasure: Added step 2 — delete RAG chunks from `vector_store` where `metadata->>'tenant_id' = ?`; added `cv_embeddings` to the hard-delete list

---

## Session 10: Design Audit — Licensing, Production Readiness & Future-Proofing

### User

> Do we need any licences or costs involved to run the whole design on local machine? Please recheck the whole design keeping all design principles to make it multi-tenant, scalable, reliable, and maintainable. The design should be production ready — make sure it's future proof.

### Copilot — Action Taken

Created `10-DESIGN-AUDIT-LICENSING.md` — comprehensive audit covering:

#### Licensing: **$0.00 total cost for local development** ✅
- Every component is open source with permissive licenses (Apache 2.0, MIT, BSD-3, PostgreSQL)
- No API keys, SaaS subscriptions, or license fees needed

#### Key technology changes from audit:
- **Redis → Valkey** (BSD-3): Fully open-source Redis fork from Linux Foundation, avoids SSPL licensing risk
- **Added MinIO** (AGPL): S3-compatible object store for local development — no AWS account needed
- **Added Resilience4j** (Apache 2.0): Circuit breaker, retry, rate limiter
- **Added OpenFeature + Flagd** (Apache 2.0): Feature flag system for gradual rollouts
- **Configurable embedding dimensions**: `${APP_EMBEDDING_DIMENSIONS:768}` — single property controls Flyway + PgVectorStore

#### 17 gaps identified and fixes documented:

| Priority | Gaps                                                                |
|----------|---------------------------------------------------------------------|
| **High** | Tenant_id validation on writes, Kafka tenant context, embedding dimension mismatch |
| **Medium** | Per-tenant rate limiting, connection pool sizing, circuit breaker library, Kafka DLT, health probes, structured logging |
| **Low** | Read replicas, API deprecation, ArchUnit, event schema evolution, feature flags, webhooks |

#### Documents updated:
- `00-SYSTEM-ARCHITECTURE.md`: Tech stack (Valkey, MinIO, Resilience4j, OpenFeature), quality attributes (observability, resilience), Docker Compose (all open-source with healthchecks), K8s deployment, component diagram, embedding dimension note
- `07-DATA-MODEL.md`: All `vector(1536)` → `vector(${APP_EMBEDDING_DIMENSIONS})` (cv_embeddings, job_embeddings, vector_store)
- New ADRs: 014–020 (Valkey, Resilience4j, MinIO, configurable dimensions, OpenFeature, ArchUnit, MDC logging)

---

## Key Architectural Decisions Log (Updated)

| # | Decision | Rationale | Date |
|---|----------|-----------|------|
| ADR-001 | **Modular monolith** (not microservices) | Simpler to start; clear package boundaries allow future extraction | 2026-02-19 |
| ADR-002 | **Database-per-region** + RLS for tenant isolation | Data residency compliance (GDPR, CCPA, PDPA); tenant_id column + Row-Level Security within each region | 2026-02-19 |
| ADR-003 | **AES-256-GCM column-level encryption** for PII | Encrypt at application layer; keys in cloud KMS | 2026-02-19 |
| ADR-004 | **pgvector** for CV/job embeddings | Stays in PostgreSQL (no separate vector DB); HNSW/IVFFlat indexes | 2026-02-19 |
| ADR-005 | **Embedded Spring Authorization Server** (not Keycloak) | Single JVM, full Java customisation, same regional DB, native Spring Boot 4 integration | 2026-02-19 |
| ADR-006 | **Spring AI with function calling** for all agents | ChatClient abstraction; provider-agnostic | 2026-02-19 |
| ADR-007 | **Kafka** for async events | Application submission, status changes, audit trail | 2026-02-19 |
| ADR-008 | **MinIO (local) / AWS S3 (prod)** for file storage | Open-source S3-compatible for local; no AWS account needed | 2026-02-19 |
| ADR-009 | **MFA deferred to v2** | Focus on core auth flows first | 2026-02-19 |
| ADR-010 | **Flyway** for database migrations | Version-controlled SQL with configurable placeholders | 2026-02-19 |
| ADR-011 | **Ollama for local AI**, OpenAI/Anthropic for production | Zero-cost local dev; profile-based switching via Spring AI | 2026-02-19 |
| ADR-012 | **Vue.js 3 SPA** for frontend | TypeScript, Vite, PrimeVue, Tailwind; OAuth 2.1 PKCE | 2026-02-19 |
| ADR-013 | **RAG with PgVectorStore in v1** | CV chunks for motivation letters and match explanations | 2026-02-19 |
| ADR-014 | **Valkey** replaces Redis | BSD-3 license; drop-in Redis replacement; avoids SSPL risk | 2026-02-19 |
| ADR-015 | **Resilience4j** for circuit breaking | Apache 2.0; retry + circuit breaker + rate limiter | 2026-02-19 |
| ADR-016 | **MinIO** for local S3 | Free, S3-compatible, web console for local dev | 2026-02-19 |
| ADR-017 | **Configurable embedding dimensions** | `${APP_EMBEDDING_DIMENSIONS}` avoids local/prod mismatch | 2026-02-19 |
| ADR-018 | **OpenFeature + Flagd** for feature flags | CNCF standard; gradual rollout capability | 2026-02-19 |
| ADR-019 | **ArchUnit** for module dependency enforcement | Prevent cross-module coupling as codebase grows | 2026-02-19 |
| ADR-020 | **MDC structured logging** with correlation IDs | traceId, tenantId, userId in every log line | 2026-02-19 |

---

## Documents Produced

```
docs/architecture/
├── 00-SYSTEM-ARCHITECTURE.md          ← System overview, tech stack, multi-tenancy, data storage
├── 01-UC-REGISTER-LOGIN.md            ← UC-01: Register / Login + Spring Auth Server
├── 02-UC-UPLOAD-CV.md                 ← UC-02: Upload & Analyse CV (+ RAG ingestion)
├── 03-UC-SEARCH-JOBS.md               ← UC-03: Search Jobs (+ RAG match explanation)
├── 04-UC-GENERATE-MOTIVATION.md       ← UC-04: Generate Motivation Letter (RAG-powered)
├── 05-UC-APPLY-JOB.md                 ← UC-05: Apply to Job
├── 06-UC-VIEW-APPLICATIONS.md         ← UC-06: View Applications
├── 07-DATA-MODEL.md                   ← Complete data model (configurable dimensions)
├── 08-UC-UI-MODULE.md                 ← UI Module: Vue.js SPA architecture
├── 09-DESIGN-REVIEW-RAG-ANALYSIS.md   ← RAG strategy with PgVectorStore
└── 10-DESIGN-AUDIT-LICENSING.md       ← Licensing, cost, production readiness, 17 gap fixes
```

---

## Session 11–13: Sprint 0 Implementation

### User

> Are we ready to start implementation? Yes, please implement.

### Copilot — Action Taken: Sprint 0 Complete ✅

**Maven BUILD SUCCESS** — 25 source files compiled, all dependencies resolved.

#### Files Created / Updated:

**Build & Infrastructure:**
- `pom.xml` — Complete rewrite: Spring Boot 4.0.2, Java 21 (LTS), Spring AI 1.0.0 (BOM), Spring Authorization Server (starter), Flyway, Resilience4j, MinIO, OpenPDF, Testcontainers, ArchUnit, etc.
- `docker-compose.yml` — Postgres+pgvector, Valkey 8, Kafka (KRaft), Ollama, MinIO, bucket initializer
- `Dockerfile` — Multi-stage build (Eclipse Temurin 21), ZGC, non-root user, health check
- `.gitignore` — Extended with Docker, Vue.js, env/secrets patterns

**Configuration:**
- `application.properties` — JPA, Flyway (with placeholder), virtual threads, Actuator, HikariCP (pool=50), Kafka, embedding dimensions
- `application-local.yml` — Ollama, MinIO, Valkey, local encryption key, CORS for localhost:5173
- `application-prod.yml` — OpenAI, AWS S3, managed Postgres/Redis/Kafka, all via env vars
- `logback-spring.xml` — Structured logging with MDC (traceId, tenantId, userId), JSON for prod

**Flyway Migrations (V1–V8):**
- `V1__create_users_profiles.sql` — users, user_profiles
- `V2__create_oauth2_auth_server_tables.sql` — oauth2_registered_client, oauth2_authorization, oauth2_authorization_consent
- `V3__create_cv_details_embeddings.sql` — cv_details, cv_embeddings (configurable vector dimension)
- `V4__create_pgvector_store_rag.sql` — vector_store + HNSW index + metadata GIN index
- `V5__create_job_listings_embeddings.sql` — job_listings, job_embeddings, saved_jobs, job_source_configs
- `V6__create_motivation_letters.sql` — motivation_letters, motivation_letter_history
- `V7__create_applications_events.sql` — applications, application_events, application_notes
- `V8__enable_rls.sql` — RLS policies on all 13 application tables

**Common Module (17 classes):**
- `common/model/BaseEntity.java` — UUID PK, tenant_id, created_at, updated_at
- `common/multitenancy/TenantContext.java` — ThreadLocal tenant holder
- `common/multitenancy/TenantContextFilter.java` — JWT → tenant extraction + PostgreSQL RLS session var + MDC
- `common/multitenancy/TenantEntityListener.java` — @PrePersist/@PreUpdate tenant validation (Gap #1)
- `common/encryption/EncryptedStringConverter.java` — AES-256-GCM JPA AttributeConverter
- `common/config/AppProperties.java` — @ConfigurationProperties for app.* (embedding, storage, encryption, cors)
- `common/config/StorageConfig.java` — MinIO/S3 client bean + auto-create buckets
- `common/config/CorsConfig.java` — CORS for Vue.js SPA
- `common/config/RootController.java` — Root health endpoint
- `common/security/SecurityConfig.java` — JWT resource server, stateless, actuator public
- `common/exception/GlobalExceptionHandler.java` — RFC 7807 ProblemDetail responses
- `common/exception/ResourceNotFoundException.java`
- `common/exception/DuplicateResourceException.java`
- `common/event/BaseEvent.java` — Tenant-aware event base class (Gap #2)
- `common/event/EventPublisher.java` — Kafka publisher with tenant_id in headers
- `common/event/Topics.java` — Kafka topic constants
- `common/rag/RagConstants.java` — RAG metadata keys, doc types, default parameters
- `common/rag/RagSearchHelper.java` — Tenant-scoped PgVectorStore search builder

**AI Prompt Templates:**
- `prompts/cv-parse.st` — CV analysis system prompt
- `prompts/motivation-letter.st` — Motivation letter generation prompt

**Module Package Structure (6 use case packages):**
- `auth/package-info.java`, `cv/package-info.java`, `jobsearch/package-info.java`
- `motivation/package-info.java`, `apply/package-info.java`, `application/package-info.java`

**Tests:**
- `ModuleDependencyTest.java` — ArchUnit rules enforcing module boundaries (Gap #13)
- `JavaAgentApplicationTests.java` — Updated with @ActiveProfiles("local")

**Vue.js Frontend (`jobagent-ui/`):**
- `package.json` — Vue 3, TypeScript, Vite 6, PrimeVue 4 (@primeuix/themes), Tailwind CSS 4, Pinia, Axios, vue-i18n 11, Chart.js, Vitest
- `tsconfig.json`, `vite.config.ts` — TypeScript strict, path aliases, API proxy to backend
- `env.d.ts`, `index.html`
- `src/main.ts` — App bootstrap with PrimeVue Aura theme
- `src/App.vue` — Root component with RouterView
- `src/assets/main.css` — Tailwind import
- `src/router/index.ts` — All routes (login, register, dashboard, cv, jobs, motivation, applications) + auth guard
- `src/api/client.ts` — Axios instance with JWT interceptor
- `src/stores/auth.ts` — Pinia auth store (Sprint 1 placeholder)
- `src/views/auth/LoginView.vue`, `RegisterView.vue` — Stubs
- `src/views/dashboard/DashboardView.vue` — Stats cards stub
- `src/views/cv/CvUploadView.vue` — Drag-and-drop stub
- `src/views/jobs/JobSearchView.vue`, `JobDetailView.vue` — Stubs
- `src/views/motivation/MotivationListView.vue` — Stub
- `src/views/applications/ApplicationsListView.vue` — Stub
- `Dockerfile` — Node 22 build + Nginx serve
- `nginx.conf` — SPA fallback, API/OAuth2 proxy, security headers, static asset caching

**Key Decisions During Implementation:**
- Java 21 (LTS) instead of 25 (not GA) — matches installed JDK
- `spring-boot-starter-oauth2-authorization-server` instead of explicit version — Spring Boot manages the version
- Spring AI 1.0.0 uses `spring-ai-starter-model-ollama` / `spring-ai-starter-model-openai` / `spring-ai-starter-vector-store-pgvector` (not the old `-spring-boot-starter` suffixed names)
- `@primeuix/themes` replaces deprecated `@primevue/themes`
- `vue-i18n` v11 replaces deprecated v10

---

## Next Steps

1. **Sprint 1** — UC-01: Register / Login (backend: Spring Auth Server config, user entity, registration endpoint, JWT token customizer + frontend: login/register forms with OAuth 2.1 PKCE)
2. **Sprint 2** — UC-02: Upload CV
3. Continue sprint-by-sprint through UC-03 → UC-06

