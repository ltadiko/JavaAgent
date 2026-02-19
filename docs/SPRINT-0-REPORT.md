# Sprint 0 — Implementation Report

## 1. Overview

Sprint 0 establishes the **project foundation** — all the infrastructure, configuration, cross-cutting concerns, and scaffolding required before implementing any business use case.

**Build Status: ✅ Maven BUILD SUCCESS** (25 Java source files compiled, all dependencies resolved)  
**Frontend Status: ✅ npm install SUCCESS** (348 packages installed)

---

## 2. Technology Stack (As Implemented)

| Layer                | Technology                          | Version      | License     |
|----------------------|-------------------------------------|-------------|-------------|
| Language             | Java (OpenJDK via Homebrew)          | 25.0.2       | GPL v2+CPE  |
| Framework            | Spring Boot                         | 4.0.2        | Apache 2.0  |
| AI Orchestration     | Spring AI (BOM)                     | 1.0.0        | Apache 2.0  |
| AI Local             | Ollama (Mistral 7B + nomic-embed-text) | latest    | MIT/Apache  |
| Database             | PostgreSQL 17 + pgvector            | pg17         | PostgreSQL/MIT |
| Caching              | Valkey (Redis-compatible)           | 8-alpine     | BSD-3       |
| Messaging            | Apache Kafka (KRaft)                | 3.7          | Apache 2.0  |
| File Storage         | MinIO (S3-compatible)               | latest       | AGPL        |
| Auth                 | Spring Authorization Server         | 7.0.2 (managed) | Apache 2.0 |
| Resilience           | Resilience4j                        | 2.3.0        | Apache 2.0  |
| Frontend             | Vue.js 3 + TypeScript + Vite 6      | 3.5 / 6.1    | MIT         |
| UI Components        | PrimeVue + @primeuix/themes (Aura)  | 4.3          | MIT         |
| CSS                  | Tailwind CSS                        | 4.0          | MIT         |

---

## 3. Project Structure

### 3.1 Root Directory

```
JavaAgent/
├── pom.xml                          ← Maven build (all dependencies)
├── Dockerfile                       ← Multi-stage Docker build (JDK 25 → JRE 25)
├── docker-compose.yml               ← Full local stack (5 services + initializer)
├── .gitignore                       ← Java, IDE, Docker, Vue.js, secrets
├── docs/
│   ├── architecture/                ← 11 design documents
│   └── chat-history/                ← Session log
├── src/                             ← Backend (Spring Boot)
└── jobagent-ui/                     ← Frontend (Vue.js SPA)
```

### 3.2 Backend Source Tree

```
src/main/java/com/jobagent/jobagent/
├── JavaAgentApplication.java        ← Spring Boot entry point
├── common/                          ← Cross-cutting concerns (17 classes)
│   ├── config/
│   │   ├── AppProperties.java       ← @ConfigurationProperties for app.*
│   │   ├── CorsConfig.java          ← CORS for Vue.js SPA
│   │   ├── RootController.java      ← GET / → service status
│   │   └── StorageConfig.java       ← MinIO/S3 client bean
│   ├── encryption/
│   │   └── EncryptedStringConverter.java  ← AES-256-GCM JPA converter
│   ├── event/
│   │   ├── BaseEvent.java           ← Tenant-aware event base class
│   │   ├── EventPublisher.java      ← Kafka publisher with tenant headers
│   │   └── Topics.java              ← Kafka topic name constants
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java    ← RFC 7807 ProblemDetail
│   │   ├── ResourceNotFoundException.java
│   │   └── DuplicateResourceException.java
│   ├── model/
│   │   └── BaseEntity.java          ← UUID PK, tenant_id, timestamps
│   ├── multitenancy/
│   │   ├── TenantContext.java        ← ThreadLocal<UUID> tenant holder
│   │   ├── TenantContextFilter.java  ← JWT → tenant + RLS + MDC
│   │   └── TenantEntityListener.java ← @PrePersist/@PreUpdate validation
│   ├── rag/
│   │   ├── RagConstants.java         ← Metadata keys, doc types, defaults
│   │   └── RagSearchHelper.java      ← Tenant-scoped PgVectorStore queries
│   └── security/
│       └── SecurityConfig.java       ← JWT resource server, stateless
├── auth/                            ← UC-01 (Sprint 1 — empty, package-info only)
├── cv/                              ← UC-02 (Sprint 2 — empty)
├── jobsearch/                       ← UC-03 (Sprint 3 — empty)
├── motivation/                      ← UC-04 (Sprint 4 — empty)
├── apply/                           ← UC-05 (Sprint 5 — empty)
└── application/                     ← UC-06 (Sprint 6 — empty)
```

### 3.3 Configuration Files

```
src/main/resources/
├── application.properties           ← Common config (all profiles)
├── application-local.yml            ← Local: Ollama, MinIO, Valkey, debug logging
├── application-prod.yml             ← Prod: OpenAI, AWS S3, env-var driven
├── logback-spring.xml               ← MDC structured logging (traceId, tenantId, userId)
├── db/migration/                    ← Flyway V1–V8
│   ├── V1__create_users_profiles.sql
│   ├── V2__create_oauth2_auth_server_tables.sql
│   ├── V3__create_cv_details_embeddings.sql
│   ├── V4__create_pgvector_store_rag.sql
│   ├── V5__create_job_listings_embeddings.sql
│   ├── V6__create_motivation_letters.sql
│   ├── V7__create_applications_events.sql
│   └── V8__enable_rls.sql
└── prompts/
    ├── cv-parse.st                  ← CV analysis system prompt
    └── motivation-letter.st         ← Motivation letter generation prompt
```

### 3.4 Frontend Source Tree

```
jobagent-ui/
├── package.json                     ← Dependencies: Vue 3, Vite, PrimeVue, Tailwind, Pinia, Axios
├── tsconfig.json                    ← TypeScript strict mode
├── vite.config.ts                   ← Path aliases, API proxy to :8080
├── index.html                       ← SPA entry
├── env.d.ts                         ← Type declarations
├── Dockerfile                       ← Node 22 build → Nginx serve
├── nginx.conf                       ← SPA fallback, API proxy, security headers
└── src/
    ├── main.ts                      ← App bootstrap (PrimeVue Aura theme)
    ├── App.vue                      ← Root component
    ├── assets/main.css              ← Tailwind import
    ├── api/client.ts                ← Axios with JWT interceptor
    ├── router/index.ts              ← All routes + auth guard
    ├── stores/auth.ts               ← Pinia auth store (placeholder)
    └── views/
        ├── auth/LoginView.vue       ← Stub (Sprint 1)
        ├── auth/RegisterView.vue    ← Stub (Sprint 1)
        ├── dashboard/DashboardView.vue ← Stats cards stub
        ├── cv/CvUploadView.vue      ← Stub (Sprint 2)
        ├── jobs/JobSearchView.vue   ← Stub (Sprint 3)
        ├── jobs/JobDetailView.vue   ← Stub (Sprint 3)
        ├── motivation/MotivationListView.vue ← Stub (Sprint 4)
        └── applications/ApplicationsListView.vue ← Stub (Sprint 6)
```

### 3.5 Test Source Tree

```
src/test/java/com/jobagent/jobagent/
├── JavaAgentApplicationTests.java   ← Context load test (@ActiveProfiles("local"))
└── ModuleDependencyTest.java        ← ArchUnit: enforces module isolation rules
```

---

## 4. Component Details

### 4.1 Docker Compose — Local Development Stack

| Service        | Image                      | Port(s)       | Purpose                          |
|----------------|----------------------------|--------------|----------------------------------|
| `postgres`     | `pgvector/pgvector:pg17`   | 5432         | PostgreSQL 17 with pgvector extension |
| `valkey`       | `valkey/valkey:8-alpine`   | 6379         | Caching, sessions, rate limits   |
| `kafka`        | `bitnami/kafka:3.7`        | 9092         | Async events (KRaft mode, no Zookeeper) |
| `ollama`       | `ollama/ollama:latest`     | 11434        | Local AI inference (chat + embeddings) |
| `minio`        | `minio/minio:latest`       | 9000, 9001   | S3-compatible file storage + web console |
| `minio-init`   | `minio/mc:latest`          | —            | Creates `jobagent-cv` and `jobagent-letters` buckets |

**Health checks** are configured on postgres, valkey, kafka, and minio. Ollama does not have a health check (it starts serving as soon as the API is ready).

**First-time setup after `docker compose up`:**
```bash
docker exec jobagent-ollama ollama pull mistral          # ~4 GB
docker exec jobagent-ollama ollama pull nomic-embed-text  # ~300 MB
```

### 4.2 Dockerfile — Backend

- **Multi-stage build**: JDK 25 for compilation → JRE 25 for runtime (smaller image)
- **Non-root user**: Runs as `jobagent` user
- **GC**: ZGC enabled (`-XX:+UseZGC`)
- **Memory**: 75% of container RAM (`-XX:MaxRAMPercentage=75.0`)
- **Health check**: `/actuator/health/liveness` via `wget`
- **Lombok note**: Explicit annotation processor path configured in `maven-compiler-plugin` (required since Java 22+)

### 4.3 Flyway Migrations (V1–V8)

| Migration | Tables Created                                       | Key Features                              |
|-----------|------------------------------------------------------|-------------------------------------------|
| **V1**    | `users`, `user_profiles`                             | pgcrypto + vector extensions, email encryption fields |
| **V2**    | `oauth2_registered_client`, `oauth2_authorization`, `oauth2_authorization_consent` | Spring Authorization Server schema |
| **V3**    | `cv_details`, `cv_embeddings`                        | `vector(${embedding_dimensions})` — configurable via Flyway placeholder |
| **V4**    | `vector_store`                                       | PgVectorStore for RAG — HNSW index + metadata GIN index |
| **V5**    | `job_listings`, `job_embeddings`, `saved_jobs`, `job_source_configs` | Composite unique (tenant, source, external_id) |
| **V6**    | `motivation_letters`, `motivation_letter_history`    | Versioned letters, encrypted text         |
| **V7**    | `applications`, `application_events`, `application_notes` | Event sourcing, unique (user, job)    |
| **V8**    | — (RLS policies)                                     | Row-Level Security on all 13 app tables via `app.current_tenant` session var |

**Embedding dimensions** are configurable: `${embedding_dimensions}` is replaced by Flyway at migration time from `spring.flyway.placeholders.embedding_dimensions` → `app.embedding.dimensions` (768 for Ollama, 1536 for OpenAI).

### 4.4 Common Module — Multi-Tenancy

**How tenant isolation works (3 layers):**

1. **Application Layer** — `TenantContextFilter` extracts `tenant_id` from JWT claims and sets `TenantContext` (ThreadLocal). `TenantEntityListener` validates tenant_id on every `@PrePersist` and `@PreUpdate`.

2. **Database Layer** — The filter also sets `SET LOCAL app.current_tenant = '<uuid>'` on the PostgreSQL connection. This activates the RLS policies from V8, which filter every SELECT/INSERT/UPDATE/DELETE to only rows where `tenant_id = current_setting('app.current_tenant')`.

3. **RAG Layer** — `RagSearchHelper` adds `filterExpression("tenant_id == '<uuid>'")` to every PgVectorStore query. The `vector_store` table does NOT use PostgreSQL RLS because it is managed by Spring AI; tenant isolation is enforced at the application layer.

### 4.5 Common Module — Encryption

`EncryptedStringConverter` uses **AES-256-GCM** with a 12-byte random IV prepended to each ciphertext, stored as Base64 in the database.

- **Usage**: `@Convert(converter = EncryptedStringConverter.class)` on entity fields containing PII (email, phone, address, letter text)
- **Key source**: `app.encryption.secret-key` — from `application-local.yml` in dev, from cloud KMS in production
- **Null-safe**: Returns `null` if the input is `null`

### 4.6 Common Module — Kafka Events

`BaseEvent` ensures every Kafka message carries `eventId`, `tenantId`, `eventType`, and `timestamp`. `EventPublisher` additionally adds `tenant_id` as a Kafka header so consumers can filter without deserializing.

**Defined topics** (in `Topics.java`):
- `jobagent.application.submitted`
- `jobagent.application.failed`
- `jobagent.application.status-changed`
- `jobagent.cv.uploaded`
- `jobagent.cv.analyzed`
- `jobagent.user.data-erased`

### 4.7 Common Module — Exception Handling

`GlobalExceptionHandler` returns **RFC 7807 Problem Details** for:

| Exception                       | HTTP Status | Scenario                          |
|---------------------------------|-------------|-----------------------------------|
| `ResourceNotFoundException`     | 404         | Entity not found by ID            |
| `MethodArgumentNotValidException` | 400       | Bean validation failure           |
| `SecurityException`             | 403         | Tenant mismatch (TenantEntityListener) |
| `AccessDeniedException`         | 403         | Spring Security denied access     |
| `DuplicateResourceException`    | 409         | e.g., applying to same job twice  |
| `Exception` (catch-all)         | 500         | Unhandled errors                  |

### 4.8 Common Module — Security

`SecurityConfig` configures:
- **Stateless sessions** (JWT only, no server-side sessions)
- **CORS** for Vue.js SPA origin
- **Public endpoints**: `/actuator/health/**`, `/actuator/info`, `/actuator/prometheus`, `/error`, `/oauth2/**`, `/.well-known/**`, `/api/v1/auth/register`
- **Protected**: Everything else requires JWT authentication
- **Password encoder**: BCrypt
- Sprint 1 will add the Authorization Server filter chain (`@Order(1)`)

### 4.9 Common Module — Structured Logging

`logback-spring.xml` configures MDC-enriched logs:

```
2026-02-19 12:00:00.123 [main] INFO  [traceId=abc123] [tenantId=uuid] [userId=sub] ClassName - message
```

- **local profile**: Human-readable pattern to console
- **prod profile**: JSON format for log aggregation (ELK/Grafana Loki)

### 4.10 Common Module — RAG

`RagConstants` defines metadata keys (`tenant_id`, `cv_id`, `section`, `doc_type`), document types (`cv_chunk`, `company_knowledge`), CV sections (`SKILLS`, `EXPERIENCE`, `EDUCATION`, `SUMMARY`, `PROJECTS`), and defaults (`topK=5`, `similarityThreshold=0.65`).

`RagSearchHelper` provides pre-built `SearchRequest` builders:
- `cvChunkSearch(query, tenantId)` — search all CV chunks for a tenant
- `cvChunkSearchByCv(query, tenantId, cvId)` — search chunks of a specific CV

Both enforce tenant isolation via `filterExpression`.

### 4.11 Configuration Profiles

| Property                         | Local (`application-local.yml`)          | Prod (`application-prod.yml`)           |
|----------------------------------|------------------------------------------|-----------------------------------------|
| Database                         | `localhost:5432/jobagent`                | `${DATABASE_URL}` (env var)             |
| Redis                            | `localhost:6379`                         | `${REDIS_HOST}:${REDIS_PORT}`           |
| Kafka                            | `localhost:9092`                         | `${KAFKA_BOOTSTRAP_SERVERS}`            |
| AI Chat                          | Ollama `mistral`                         | OpenAI `gpt-4o`                         |
| AI Embedding                     | Ollama `nomic-embed-text` (768d)         | OpenAI `text-embedding-3-small` (1536d) |
| File Storage                     | MinIO `localhost:9000`                   | AWS S3 `${S3_ENDPOINT}`                 |
| Encryption Key                   | `local-dev-key-32-chars-exactly!!`       | `${ENCRYPTION_SECRET_KEY}` (KMS)        |
| Embedding Dimensions             | 768                                      | 1536                                    |
| CORS                             | `http://localhost:5173`                  | `${CORS_ALLOWED_ORIGINS}`               |
| Logging                          | DEBUG (com.jobagent, spring.ai)          | INFO / WARN                             |

### 4.12 Tests

| Test                              | Type     | Purpose                                           |
|-----------------------------------|----------|----------------------------------------------------|
| `JavaAgentApplicationTests`       | Integration | Context load test (verifies Spring wiring)       |
| `ModuleDependencyTest`            | ArchUnit | Enforces: auth ✗→ business modules; cv ✗→ motivation; cv ✗→ apply; motivation ✗→ apply; application ✗→ apply |

### 4.13 Vue.js Frontend

**Scaffolding only** — all views are stubs that will be implemented in their respective sprints.

| Feature                | Implementation                              |
|------------------------|---------------------------------------------|
| **Routing**            | 8 routes: /, /login, /register, /dashboard, /cv, /jobs, /jobs/:id, /motivation-letters, /applications |
| **Auth guard**         | `router.beforeEach` checks `localStorage.access_token` (will be replaced by Pinia store + PKCE in Sprint 1) |
| **API client**         | Axios with `Bearer` JWT interceptor, auto-redirect to `/login` on 401 |
| **State management**   | Pinia `useAuthStore` — token, user, isAuthenticated, setToken, logout |
| **UI framework**       | PrimeVue 4 with Aura theme preset          |
| **CSS**                | Tailwind CSS 4 via `@tailwindcss/vite`      |
| **Proxy**              | Vite dev server proxies `/api`, `/oauth2`, `/.well-known` to `:8080` |
| **Production build**   | Multi-stage Docker: Node 22 build → Nginx with SPA fallback, API proxy, security headers |

---

## 5. Known Issues & Notes for Review

### 5.1 Lombok Deprecation Warning (non-blocking)

When compiling with Java 25, Lombok produces a warning:
```
WARNING: sun.misc.Unsafe::objectFieldOffset has been called by lombok.permit.Permit
WARNING: sun.misc.Unsafe::objectFieldOffset will be removed in a future release
```
This is a known Lombok issue. It does **not** affect compilation or runtime. The explicit `annotationProcessorPaths` configuration in `maven-compiler-plugin` is **required** since Java 22+ no longer auto-discovers annotation processors on the classpath.

### 5.2 Ollama Models Not Pre-Loaded

After `docker compose up`, you must manually pull Ollama models:
```bash
docker exec jobagent-ollama ollama pull mistral          # ~4 GB
docker exec jobagent-ollama ollama pull nomic-embed-text  # ~300 MB
```

This is intentional (models are ~4.3 GB total) and only required once.

### 5.3 BouncyCastle Apache Snapshots Warning (non-blocking)

Maven outputs PKIX warnings when checking Apache Snapshots repository for BouncyCastle metadata. This is a certificate issue with the snapshot repo and does **not** affect the build — all dependencies resolve from Maven Central.

### ~~5.4 Resolved Issues~~

The following issues from earlier sessions have been resolved:
- ✅ **Java version** — `pom.xml` has `<java.version>25</java.version>`, `.zshrc` defaults to Java 25, `Dockerfile` uses `eclipse-temurin:25`
- ✅ **Authorization Server** — Uses `spring-boot-starter-oauth2-authorization-server` (Spring Boot managed)
- ✅ **Extra pom.xml property** — Removed `spring-boot-starter-parent.version`
- ✅ **npm deprecations** — Updated `@primeuix/themes` and `vue-i18n` v11

---

## 6. How to Run Locally

```bash
# 1. Start infrastructure
docker compose up -d

# 2. Wait for services to be healthy
docker compose ps   # all should show "healthy" or "running"

# 3. Pull AI models (first time only)
docker exec jobagent-ollama ollama pull mistral
docker exec jobagent-ollama ollama pull nomic-embed-text

# 4. Run backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 5. Run frontend (separate terminal)
cd jobagent-ui && npm install && npm run dev

# 6. Access
#    Backend API:   http://localhost:8080
#    Frontend SPA:  http://localhost:5173
#    MinIO Console: http://localhost:9001 (minioadmin/minioadmin)
```

---

## 7. What's Next — Sprint 1: UC-01 Register / Login

Sprint 1 will implement:

**Backend:**
- Spring Authorization Server configuration (`@Order(1)` filter chain)
- JWT token customizer (add `tenant_id`, `region` claims)
- `User` JPA entity (with encrypted email)
- `UserService` — registration, lookup by email hash
- `AuthController` — `POST /api/v1/auth/register`
- OAuth2 client registration for the Vue.js SPA (PKCE)
- `UserProfile` entity and endpoint

**Frontend:**
- Login page with OAuth 2.1 PKCE flow
- Registration form (name, email, password, country)
- Auth store (replace localStorage with proper token management)
- App layout with navigation sidebar

**Tests:**
- Integration tests with Testcontainers (Postgres)
- Security tests (registration, login, JWT validation)
