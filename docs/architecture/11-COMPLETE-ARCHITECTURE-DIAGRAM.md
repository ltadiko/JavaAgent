# 11 — Complete Architecture Diagram

> **One diagram to rule them all.** This document provides a single, comprehensive view of every component, data flow, and interaction in the JobAgent platform.

---

## 1. Full System Architecture (All Components)

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                    CLIENTS                                               │
│                                                                                          │
│   ┌────────────────────────────────┐     ┌───────────────────────────────────────┐       │
│   │   Vue.js SPA (jobagent-ui)     │     │   Mobile / Third-Party Clients        │       │
│   │   ┌─────────────────────────┐  │     │   (future — same API)                 │       │
│   │   │ PrimeVue 4 + Tailwind 4│  │     └───────────────────────────────────────┘       │
│   │   │ Pinia State Management  │  │                                                     │
│   │   │ Axios HTTP Client       │  │                                                     │
│   │   │ Vue Router (8 routes)   │  │                                                     │
│   │   └─────────────────────────┘  │                                                     │
│   │   Vite dev :5173 / Nginx prod  │                                                     │
│   └──────────────┬─────────────────┘                                                     │
│                  │  HTTPS                                                                 │
│                  │  OAuth 2.1 PKCE + Bearer JWT                                           │
└──────────────────┼───────────────────────────────────────────────────────────────────────┘
                   │
┌──────────────────▼───────────────────────────────────────────────────────────────────────┐
│                            EDGE / INGRESS LAYER                                          │
│                                                                                          │
│   ┌────────────────────────────────────────────────────────────────────────────────┐     │
│   │  API Gateway / Load Balancer / K8s Ingress                                     │     │
│   │  • TLS 1.3 termination                                                         │     │
│   │  • Geo-routing (EU / US / APAC) based on JWT `region` claim                    │     │
│   │  • Rate limiting (per tenant)                                                  │     │
│   │  • JWT signature validation                                                    │     │
│   └──────────────┬─────────────────────────────────┬───────────────────────────────┘     │
│                  │                                  │                                     │
└──────────────────┼──────────────────────────────────┼─────────────────────────────────────┘
                   │                                  │
    ╔══════════════▼══════════════════╗   ╔═══════════▼══════════════════════╗
    ║    REGION: EU                    ║   ║    REGION: US (mirror)           ║
    ║    (K8s namespace: jobagent-eu)  ║   ║    (K8s namespace: jobagent-us)  ║
    ║                                  ║   ║                                  ║
    ║  ┌───────────────────────────┐   ║   ║  ┌───────────────────────────┐   ║
    ║  │  SPRING BOOT APPLICATION  │   ║   ║  │  SPRING BOOT APPLICATION  │   ║
    ║  │  (Java 25, Virtual Threads│   ║   ║  │  (identical deployment)   │   ║
    ║  │   Spring Boot 4.0.2)      │   ║   ║  └───────────────────────────┘   ║
    ║  │                           │   ║   ║        (same as EU)              ║
    ║  │  ┌─────────────────────┐  │   ║   ╚══════════════════════════════════╝
    ║  │  │   COMMON MODULE     │  │   ║
    ║  │  │  ┌───────────────┐  │  │   ║
    ║  │  │  │ TenantContext  │  │  │   ║
    ║  │  │  │ Filter → RLS  │  │  │   ║
    ║  │  │  ├───────────────┤  │  │   ║
    ║  │  │  │ Security      │  │  │   ║
    ║  │  │  │ Config (JWT)  │  │  │   ║
    ║  │  │  ├───────────────┤  │  │   ║
    ║  │  │  │ AES-256-GCM   │  │  │   ║
    ║  │  │  │ Encryption    │  │  │   ║
    ║  │  │  ├───────────────┤  │  │   ║
    ║  │  │  │ Resilience4j  │  │  │   ║
    ║  │  │  │ (CB/Retry/RL) │  │  │   ║
    ║  │  │  ├───────────────┤  │  │   ║
    ║  │  │  │ RAG Helpers   │  │  │   ║
    ║  │  │  │ (PgVector)    │  │  │   ║
    ║  │  │  ├───────────────┤  │  │   ║
    ║  │  │  │ EventPublisher│  │  │   ║
    ║  │  │  │ (Kafka+tenant)│  │  │   ║
    ║  │  │  ├───────────────┤  │  │   ║
    ║  │  │  │ GlobalExcept. │  │  │   ║
    ║  │  │  │ (RFC 7807)    │  │  │   ║
    ║  │  │  └───────────────┘  │  │   ║
    ║  │  └─────────────────────┘  │   ║
    ║  │                           │   ║
    ║  │  ┌─────────────────────┐  │   ║
    ║  │  │  BUSINESS MODULES   │  │   ║
    ║  │  │                     │  │   ║
    ║  │  │  ┌───────────────┐  │  │   ║
    ║  │  │  │ UC-01 AUTH    │  │  │   ║
    ║  │  │  │ Spring Auth   │──│──│───║──► OAuth 2.1 / OIDC endpoints
    ║  │  │  │ Server        │  │  │   ║   /oauth2/authorize, /oauth2/token
    ║  │  │  │ JWT Customizer│  │  │   ║   /.well-known/openid-configuration
    ║  │  │  ├───────────────┤  │  │   ║
    ║  │  │  │ UC-02 CV      │  │  │   ║
    ║  │  │  │ CVAnalyzer    │──│──│───║──► Ollama/OpenAI (parse CV)
    ║  │  │  │ Agent         │──│──│───║──► MinIO/S3 (store PDF)
    ║  │  │  │ (Spring AI)   │──│──│───║──► PgVectorStore (RAG ingest)
    ║  │  │  ├───────────────┤  │  │   ║
    ║  │  │  │ UC-03 JOBS    │  │  │   ║
    ║  │  │  │ JobFinder     │──│──│───║──► External job sites (JSoup scrape)
    ║  │  │  │ Agent         │──│──│───║──► Ollama/OpenAI (match explain)
    ║  │  │  │ (Spring AI)   │──│──│───║──► PgVectorStore (CV↔Job match)
    ║  │  │  ├───────────────┤  │  │   ║
    ║  │  │  │ UC-04 MOTIV.  │  │  │   ║
    ║  │  │  │ Motivation    │──│──│───║──► Ollama/OpenAI (generate letter)
    ║  │  │  │ WriterAgent   │──│──│───║──► PgVectorStore (RAG: CV context)
    ║  │  │  │ (Spring AI)   │──│──│───║──► MinIO/S3 (store PDF)
    ║  │  │  ├───────────────┤  │  │   ║
    ║  │  │  │ UC-05 APPLY   │  │  │   ║
    ║  │  │  │ ApplyAgent    │──│──│───║──► Email (SMTP)
    ║  │  │  │ (Spring AI)   │──│──│───║──► Job portals (Playwright/API)
    ║  │  │  ├───────────────┤  │  │   ║
    ║  │  │  │ UC-06 APPS    │  │  │   ║
    ║  │  │  │ Dashboard     │  │  │   ║   (read-only views, stats)
    ║  │  │  │ & Stats       │  │  │   ║
    ║  │  │  └───────────────┘  │  │   ║
    ║  │  └─────────────────────┘  │   ║
    ║  └───────────────────────────┘   ║
    ║              │                    ║
    ║  ════════════▼════════════════    ║
    ║       DATA & INFRA LAYER         ║
    ║  ════════════════════════════     ║
    ║                                  ║
    ║  ┌─────────────────────────┐     ║
    ║  │ PostgreSQL 17 + pgvector│     ║
    ║  │─────────────────────────│     ║
    ║  │ • users, user_profiles  │     ║
    ║  │ • oauth2_* (3 tables)   │     ║
    ║  │ • cv_details,           │     ║
    ║  │   cv_embeddings         │     ║
    ║  │ • vector_store (RAG)    │     ║
    ║  │ • job_listings,         │     ║
    ║  │   job_embeddings,       │     ║
    ║  │   saved_jobs,           │     ║
    ║  │   job_source_configs    │     ║
    ║  │ • motivation_letters,   │     ║
    ║  │   motivation_letter_    │     ║
    ║  │   history               │     ║
    ║  │ • applications,         │     ║
    ║  │   application_events,   │     ║
    ║  │   application_notes     │     ║
    ║  │ ┌─────────────────────┐ │     ║
    ║  │ │ RLS Policies (V8)   │ │     ║
    ║  │ │ 13 tables protected │ │     ║
    ║  │ │ app.current_tenant  │ │     ║
    ║  │ └─────────────────────┘ │     ║
    ║  │ ┌─────────────────────┐ │     ║
    ║  │ │ HNSW Vector Indexes │ │     ║
    ║  │ │ cv_embeddings       │ │     ║
    ║  │ │ job_embeddings      │ │     ║
    ║  │ │ vector_store        │ │     ║
    ║  │ └─────────────────────┘ │     ║
    ║  │ Flyway V1–V8 managed    │     ║
    ║  └─────────────────────────┘     ║
    ║                                  ║
    ║  ┌─────────────────────────┐     ║
    ║  │ MinIO / AWS S3          │     ║
    ║  │─────────────────────────│     ║
    ║  │ Bucket: jobagent-cv     │     ║
    ║  │  └ /{tenant_id}/{cv_id} │     ║
    ║  │ Bucket: jobagent-letters│     ║
    ║  │  └ /{tenant_id}/{id}.pdf│     ║
    ║  │ SSE-S3 encryption       │     ║
    ║  │ Signed URLs (5-min TTL) │     ║
    ║  └─────────────────────────┘     ║
    ║                                  ║
    ║  ┌─────────────────────────┐     ║
    ║  │ Valkey 8 (Redis)        │     ║
    ║  │─────────────────────────│     ║
    ║  │ • Session cache         │     ║
    ║  │ • Rate limiting         │     ║
    ║  │ • Job listing cache     │     ║
    ║  │ • Per-tenant throttle   │     ║
    ║  └─────────────────────────┘     ║
    ║                                  ║
    ╚════════════════╤═════════════════╝
                     │
    ┌────────────────▼────────────────────────────────────────────────┐
    │                   SHARED SERVICES                               │
    │                                                                 │
    │  ┌─────────────────────────┐  ┌──────────────────────────────┐  │
    │  │ Apache Kafka (KRaft)    │  │  AI Providers                │  │
    │  │─────────────────────────│  │──────────────────────────────│  │
    │  │ Topics:                 │  │                              │  │
    │  │ • application.submitted │  │  LOCAL (profile: local)      │  │
    │  │ • application.failed    │  │  ┌────────────────────────┐  │  │
    │  │ • application.status-   │  │  │ Ollama                 │  │  │
    │  │   changed               │  │  │ ├ mistral (7B) — chat  │  │  │
    │  │ • cv.uploaded           │  │  │ └ nomic-embed-text     │  │  │
    │  │ • cv.analyzed           │  │  │   — embeddings (768d)  │  │  │
    │  │ • user.data-erased      │  │  └────────────────────────┘  │  │
    │  │                         │  │                              │  │
    │  │ All events carry:       │  │  CLOUD (profile: prod)       │  │
    │  │  • tenant_id (header)   │  │  ┌────────────────────────┐  │  │
    │  │  • eventId, timestamp   │  │  │ OpenAI / Anthropic     │  │  │
    │  │                         │  │  │ ├ gpt-4o — chat        │  │  │
    │  │ Dead Letter Topic (DLT) │  │  │ └ text-embedding-3-    │  │  │
    │  │ for failed messages     │  │  │   small (1536d)        │  │  │
    │  └─────────────────────────┘  │  └────────────────────────┘  │  │
    │                               │                              │  │
    │                               │  (Spring AI ChatClient       │  │
    │                               │   abstracts provider swap    │  │
    │                               │   via profile — zero code    │  │
    │                               │   changes)                   │  │
    │                               └──────────────────────────────┘  │
    │                                                                 │
    │  ┌───────────────────────────────────────────────────────────┐  │
    │  │  OBSERVABILITY                                            │  │
    │  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────────┐  │  │
    │  │  │ Micrometer   │ │ Prometheus   │ │ Grafana          │  │  │
    │  │  │ + Actuator   │→│ /metrics     │→│ Dashboards       │  │  │
    │  │  └──────────────┘ └──────────────┘ └──────────────────┘  │  │
    │  │  ┌──────────────────────────────────────────────────────┐ │  │
    │  │  │ Structured Logging (logback-spring.xml)              │ │  │
    │  │  │ MDC: traceId, tenantId, userId                       │ │  │
    │  │  │ Local: human-readable │ Prod: JSON → ELK/Loki        │ │  │
    │  │  └──────────────────────────────────────────────────────┘ │  │
    │  └───────────────────────────────────────────────────────────┘  │
    └─────────────────────────────────────────────────────────────────┘
```

---

## 2. Request Flow (End-to-End)

```
  Browser                Vue.js SPA              API Gateway         Spring Boot App            PostgreSQL         AI Provider
    │                       │                        │                     │                       │                  │
    │  1. Navigate          │                        │                     │                       │                  │
    │──────────────────────►│                        │                     │                       │                  │
    │                       │  2. PKCE /oauth2/auth  │                     │                       │                  │
    │                       │───────────────────────►│                     │                       │                  │
    │                       │                        │──────3. Forward────►│                       │                  │
    │                       │                        │                     │  4. Validate creds     │                  │
    │                       │                        │                     │──────────────────────►│                  │
    │                       │                        │                     │◄─────────────────────│                  │
    │                       │                        │                     │  5. Issue JWT           │                  │
    │                       │                        │                     │     (sub, tenant_id,    │                  │
    │                       │◄───────────────────��──────────────────────── │      region claims)     │                  │
    │                       │  6. Store token (Pinia)│                     │                       │                  │
    │                       │                        │                     │                       │                  │
    │  7. API call          │                        │                     │                       │                  │
    │  (e.g., search jobs)  │  8. GET /api/v1/jobs   │                     │                       │                  │
    │──────────────────────►│──Bearer JWT───────────►│                     │                       │                  │
    │                       │                        │──9. Route by region─►│                       │                  │
    │                       │                        │                     │ 10. TenantContextFilter│                  │
    │                       │                        │                     │     JWT → tenant_id    │                  │
    │                       │                        │                     │     SET app.current_   │                  │
    │                       │                        │                     │     tenant (RLS)       │                  │
    │                       │                        │                     │     MDC logging        │                  │
    │                       │                        │                     │                       │                  │
    │                       │                        │                     │ 11. Service logic      │                  │
    │                       │                        │                     │──────────────────────►│                  │
    │                       │                        │                     │     (RLS-filtered      │                  │
    │                       │                        │                     │      query)            │                  │
    │                       │                        │                     │◄─────────────────────│                  │
    │                       │                        │                     │                       │                  │
    │                       │                        │                     │ 12. AI call            │                  │
    │                       │                        │                     │  (Resilience4j CB)     │                  │
    │                       │                        │                     │────────────────────────────────────────►│
    │                       │                        │                     │◄───────────────────────────────────────│
    │                       │                        │                     │                       │                  │
    │                       │◄──────────────────────────13. JSON response──│                       │                  │
    │◄─────────────────────│                        │                     │                       │                  │
    │  14. Render UI        │                        │                     │                       │                  │
```

---

## 3. Multi-Tenancy Isolation (3 Layers)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        TENANT ISOLATION STACK                               │
│                                                                             │
│  LAYER 1: APPLICATION                                                       │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  TenantContextFilter (jakarta.servlet.Filter, @Order(1))           │    │
│  │  ┌──────────────┐   ┌──────────────────┐   ┌──────────────────┐   │    │
│  │  │ Extract JWT  │──►│ TenantContext    │──►│ MDC Logging      │   │    │
│  │  │ tenant_id    │   │ .setTenantId()   │   │ tenantId, userId │   │    │
│  │  └──────────────┘   └──────────────────┘   └──────────────────┘   │    │
│  │                                                                    │    │
│  │  TenantEntityListener (@PrePersist / @PreUpdate)                   │    │
│  │  ┌──────────────────────────────────────────────────────────┐     │    │
│  │  │ IF entity.tenantId == null → auto-set from context       │     │    │
│  │  │ IF entity.tenantId != context → throw SecurityException  │     │    │
│  │  └──────────────────────────────────────────────────────────┘     │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│  LAYER 2: DATABASE (PostgreSQL RLS)                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  SET LOCAL app.current_tenant = '<uuid>';   (per connection)       │    │
│  │                                                                    │    │
│  │  CREATE POLICY tenant_isolation_xxx ON <table>                     │    │
│  │    USING (tenant_id = current_setting('app.current_tenant')::uuid) │    │
│  │                                                                    │    │
│  │  Applied to all 13 application tables (V8 migration)               │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│  LAYER 3: RAG (PgVectorStore)                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  RagSearchHelper.cvChunkSearch(query, tenantId)                    │    │
│  │  → filterExpression("tenant_id == '<uuid>' && doc_type == '...'") │    │
│  │  (application-layer filter, not RLS — Spring AI manages table)     │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│  LAYER 4: FILE STORAGE                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  S3 path: /{region}/{tenant_id}/cv/{file_id}                       │    │
│  │  IAM policy restricts cross-tenant access                          │    │
│  │  Signed URLs with 5-minute TTL for downloads                       │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Data Flow by Use Case

```
UC-01: REGISTER / LOGIN
═══════════════════════
  User ──► POST /api/v1/auth/register ──► UserService ──► PostgreSQL (users)
  User ──► OAuth2 PKCE flow ──► Spring Auth Server ──► JWT (tenant_id, region)

UC-02: UPLOAD CV
════════════════
  User ──► POST /api/v1/cv/upload ──► CvController
       │                                │
       ▼                                ▼
  MinIO/S3 (raw file)         CVAnalyzerAgent (Spring AI)
                                │
                    ┌───────────┼───────────┐
                    ▼           ▼           ▼
               Tika (text)  LLM (parse)  Embedding Model
                    │           │           │
                    ▼           ▼           ▼
              extracted_text  parsed_json  cv_embeddings (pgvector)
                                           │
                                           ▼
                                    PgVectorStore (RAG chunks)
                                    metadata: tenant_id, cv_id, section

UC-03: SEARCH JOBS
══════════════════
  User ──► GET /api/v1/jobs/search ──► JobSearchController
       │                                    │
       ▼                                    ▼
  JobFinderAgent ─────────► External Sites (JSoup scrape)
       │                         │
       ▼                         ▼
  PgVectorStore ◄──── cv_embeddings ↔ job_embeddings (cosine similarity)
       │
       ▼
  LLM (match explanation) ──► "85% match: 4/5 required skills present..."
       │
       ▼
  job_listings (PostgreSQL) ──► Response with match_score + explanation

UC-04: GENERATE MOTIVATION LETTER
══════════════════════════════════
  User ──► POST /api/v1/motivation-letters/generate
       │                    │
       ▼                    ▼
  PgVectorStore         job_listings
  (retrieve CV chunks   (job description)
   by tenant + cv_id)       │
       │                    │
       └────────┬───────────┘
                ▼
  MotivationWriterAgent (Spring AI)
  Prompt: cv-context + job-description + tone + language
                │
                ▼
  LLM (generate letter) ──► motivation_letters (PostgreSQL)
                            ──► MinIO/S3 (PDF via OpenPDF)

UC-05: APPLY TO JOB
═══════════════════
  User ──► POST /api/v1/applications
       │                    │
       ▼                    ▼
  applications          ApplyAgent (Spring AI)
  (status: PENDING)         │
                    ┌───────┼───────┐
                    ▼       ▼       ▼
               Email    API POST   Playwright
               (SMTP)   (REST)     (form fill)
                    │       │       │
                    └───────┼───────┘
                            ▼
                    Kafka: application.submitted / application.failed
                            │
                            ▼
                    applications (status: SUBMITTED / FAILED)

UC-06: VIEW APPLICATIONS
═══════════════════════
  User ──► GET /api/v1/applications ──► ApplicationController
       │                                    │
       ▼                                    ▼
  applications + application_events    Stats aggregation
  (filtered by tenant_id via RLS)      (by status, by week)
       │                                    │
       ▼                                    ▼
  Dashboard response (list + stats + timeline)
```

---

## 5. Technology Map (What Runs Where)

### 5.1 Local Development (`docker compose up`)

```
┌─────────────────────────────────────────────────────────────────────┐
│  docker compose up                                                   │
│                                                                      │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────────────┐ │
│  │ postgres        │  │ valkey          │  │ kafka                  │ │
│  │ pgvector:pg17   │  │ valkey:8-alpine │  │ bitnami/kafka:3.7      │ │
│  │ :5432           │  │ :6379           │  │ :9092 (KRaft, no ZK)   │ │
│  └────────────────┘  └────────────────┘  └────────────────────────┘ │
│                                                                      │
│  ┌────────────────┐  ┌────────────────────────────────────────────┐ │
│  │ ollama          │  │ minio + minio-init                        │ │
│  │ ollama:latest   │  │ minio:latest                              │ │
│  │ :11434          │  │ :9000 (S3 API) :9001 (console)            │ │
│  │ mistral (chat)  │  │ Buckets: jobagent-cv, jobagent-letters    │ │
│  │ nomic-embed-text│  │ Creds: minioadmin / minioadmin            │ │
│  └────────────────┘  └────────────────────────────────────────────┘ │
│                                                                      │
│  Total cost: $0.00 — all open source (BSD, Apache, MIT, AGPL)       │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│  Developer machine (outside Docker)                                  │
│                                                                      │
│  ┌──────────────────────────┐   ┌─────────────────────────────────┐ │
│  │ Spring Boot App           │   │ Vue.js Dev Server               │ │
│  │ ./mvnw spring-boot:run    │   │ cd jobagent-ui && npm run dev   │ │
│  │ -Dprofiles=local          │   │ :5173                           │ │
│  │ :8080                     │   │ Vite proxy → :8080              │ │
│  │ Java 25 + Virtual Threads │   │ PrimeVue + Tailwind + TypeScript│ │
│  └──────────────────────────┘   └─────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

### 5.2 Production (Kubernetes per Region)

```
┌─────────────────────────────────────────────────────────────────────┐
│  K8s Cluster (per region: EU, US, APAC)                              │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ Deployment: jobagent-app  (replicas: 2–10, HPA)                │ │
│  │  • eclipse-temurin:25-jre-alpine                               │ │
│  │  • SPRING_PROFILES_ACTIVE=prod                                 │ │
│  │  • Liveness:  /actuator/health/liveness                        │ │
│  │  • Readiness: /actuator/health/readiness                       │ │
│  └────────────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ Deployment: jobagent-ui   (replicas: 2–4, Nginx)               │ │
│  └────────────────────────────────────────────────────────────────┘ │
│  ┌─────────────────┐ ┌──────────────┐ ┌────────────────────────┐  │
│  │ Managed Postgres │ │ Managed Redis│ │ Managed Kafka          │  │
│  │ (AWS RDS/Aurora) │ │ (ElastiCache)│ │ (MSK / Confluent)     │  │
│  └─────────────────┘ └──────────────┘ └────────────────────────┘  │
│  ┌─────────────────┐ ┌──────────────────────────────────────────┐  │
│  │ AWS S3           │ │ Prometheus + Grafana                     │  │
│  └─────────────────┘ └──────────────────────────────────────────┘  │
│                                                                      │
│  AI: OpenAI API (external, pay-per-token)                            │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 6. Security Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                     SECURITY LAYERS                                  │
│                                                                      │
│  ┌─── TRANSPORT ──────────────────────────────────────────────────┐ │
│  │  TLS 1.3 (all external traffic)                                │ │
│  │  mTLS (internal service-to-service in K8s — optional)          │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌─── AUTHENTICATION ────────────────────────────────────────────┐  │
│  │  Spring Authorization Server (embedded)                        │  │
│  │  • OAuth 2.1 + PKCE (public client — Vue.js SPA)              │  │
│  │  • JWT access tokens (signed RS256)                            │  │
│  │  • Custom claims: sub, tenant_id, region, email, roles         │  │
│  │  • Refresh token rotation                                      │  │
│  │  • OIDC Discovery: /.well-known/openid-configuration           │  │
│  └────────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌─── AUTHORIZATION ────────────────────────────────────────────┐   │
│  │  SecurityConfig: stateless, JWT resource server                │   │
│  │  • Public: /actuator/health, /oauth2/**, /api/v1/auth/register│   │
│  │  • Protected: everything else (Bearer JWT required)            │   │
│  │  • Tenant isolation: TenantContextFilter + RLS                 │   │
│  │  • Entity validation: TenantEntityListener                     │   │
│  └────────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌─── ENCRYPTION AT REST ────────────────────────────────────────┐  │
│  │  • DB columns: AES-256-GCM (EncryptedStringConverter)          │  │
│  │    Applied to: email, phone, address, letter_text              │  │
│  │  • Passwords: BCrypt (PasswordEncoder)                         │  │
│  │  • S3 files: SSE-S3 (server-side encryption)                   │  │
│  │  • Key management: app config (local) / Cloud KMS (prod)       │  │
│  └────────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌─── API SAFETY ────────────────────────────────────────────────┐  │
│  │  • CSRF disabled (stateless JWT, no cookies)                   │  │
│  │  • CORS restricted to SPA origin                               │  │
│  │  • Resilience4j rate limiter (per tenant)                      │  │
│  │  • Input validation (Spring Validation + Bean Validation)      │  │
│  │  • RFC 7807 ProblemDetail error responses                      │  │
│  └────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 7. Module Dependency Rules (ArchUnit-Enforced)

```
 ┌────────────┐
 │   common   │ ◄──── ALL modules depend on common
 └─────┬──────┘
       │ (allowed)
       │
 ┌─────▼──────┐   ┌──────────┐   ┌──────────────┐   ┌──────────┐   ┌─────────┐   ┌─────────────┐
 │    auth     │   │    cv     │   │  jobsearch   │   │motivation│   │  apply   │   │ application  │
 └─────────────┘   └──────────┘   └──────────────┘   └──────────┘   └─────────┘   └─────────────┘

 FORBIDDEN (enforced by ModuleDependencyTest):
   auth ───✗───► cv, jobsearch, motivation, apply, application
   cv ────✗───► motivation, apply
   motivation ─✗─► apply
   application ─✗─► apply

 ALLOWED (via Kafka events between modules):
   cv ───event───► jobsearch    (cv.analyzed → trigger re-ranking)
   motivation ───event───► apply (letter.approved → trigger application)
```

---

## 8. Legend

| Symbol | Meaning |
|--------|---------|
| `──►` | Synchronous call (HTTP / method call) |
| `──event──►` | Asynchronous event (Kafka) |
| `═���═` | Section separator |
| `╔���═╗` | Region boundary |
| `┌──┐` | Component box |
| `✗` | Forbidden dependency |
| RLS | PostgreSQL Row-Level Security |
| CB | Circuit Breaker (Resilience4j) |
| PKCE | Proof Key for Code Exchange |
| HNSW | Hierarchical Navigable Small World (vector index) |
| DLT | Dead Letter Topic (Kafka) |
