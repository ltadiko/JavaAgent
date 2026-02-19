# 10 — Design Audit: Licensing, Cost, Production Readiness & Future-Proofing

## 1. Licensing & Cost Analysis — Local Development

### 1.1 Complete Stack — All Open Source, Zero Cost ✅

| Component                          | License          | Cost (Local) | Cost (Production)        | Notes                                      |
|------------------------------------|------------------|-------------|--------------------------|---------------------------------------------|
| **Java 25** (OpenJDK)             | GPL v2 + CPE     | Free        | Free                     | Use Eclipse Temurin / Adoptium distribution |
| **Spring Boot 4**                 | Apache 2.0       | Free        | Free                     | Open source                                 |
| **Spring AI 1.x**                 | Apache 2.0       | Free        | Free                     | Open source                                 |
| **Spring Authorization Server**   | Apache 2.0       | Free        | Free                     | Embedded — no separate license              |
| **PostgreSQL 17**                 | PostgreSQL License | Free      | Free (self-hosted) / Managed ($) | pgvector is also MIT license          |
| **pgvector**                      | MIT              | Free        | Free                     | Extension for PostgreSQL                    |
| **Redis 7**                       | BSD-3-Clause (≤7.2) / SSPL (≥7.4) | Free | Free (self-hosted) / Use **Valkey** for fully open | See §1.2 |
| **Apache Kafka**                  | Apache 2.0       | Free        | Free (self-hosted) / Managed ($) | Bitnami image for local              |
| **Ollama**                        | MIT              | Free        | N/A (local only)         | Local AI inference                          |
| **Mistral 7B** (model)           | Apache 2.0       | Free        | N/A                      | Open-weight model                           |
| **Llama 3.1 8B** (model)         | Meta Llama 3.1 Community License | Free | N/A              | Free for commercial use < 700M MAU          |
| **nomic-embed-text** (model)     | Apache 2.0       | Free        | N/A                      | Open-source embedding model                 |
| **Vue.js 3**                     | MIT              | Free        | Free                     | Open source                                 |
| **Vite 6**                       | MIT              | Free        | Free                     |                                             |
| **PrimeVue 4**                   | MIT              | Free        | Free                     | Community edition — MIT. PrimeTek also offers paid templates (optional, cosmetic only) |
| **Tailwind CSS 4**               | MIT              | Free        | Free                     |                                             |
| **Docker**                       | Apache 2.0       | Free        | Free                     | Docker Engine is open source; Docker Desktop is free for < 250 employees / < $10M revenue |
| **Docker Compose**               | Apache 2.0       | Free        | Free                     |                                             |
| **Nginx**                        | BSD-2-Clause     | Free        | Free                     | For serving Vue.js SPA in production        |
| **Apache Tika**                  | Apache 2.0       | Free        | Free                     | CV text extraction                          |
| **OpenPDF**                      | LGPL / MPL       | Free        | Free                     | PDF generation for motivation letters       |
| **Flyway** (Community)          | Apache 2.0       | Free        | Free                     | Community edition is fully open source       |
| **Playwright** (testing)         | Apache 2.0       | Free        | Free                     | E2E testing                                 |
| **Vitest** (testing)             | MIT              | Free        | Free                     | Vue.js unit testing                         |
| **Testcontainers**               | Apache 2.0       | Free        | Free                     | Integration testing                         |
| **Micrometer**                   | Apache 2.0       | Free        | Free                     | Metrics                                     |
| **Prometheus + Grafana**         | Apache 2.0       | Free        | Free (self-hosted)       | Observability                               |

### 1.2 Redis License Consideration

Redis changed to **SSPL** (Server Side Public License) from v7.4+, which restricts cloud providers from offering it as a managed service. For **local development**, this has **zero impact** — you can use Redis freely.

For production, options:
- **Redis ≤ 7.2**: BSD-3-Clause (fully open)
- **Valkey** (Linux Foundation fork): BSD-3-Clause — drop-in Redis replacement, fully open source
- **AWS ElastiCache / Azure Cache**: Already licensed for managed use

**Recommendation:** Use **Valkey** in Docker Compose and production to avoid any SSPL concerns:

```yaml
redis:
  image: valkey/valkey:8-alpine    # Drop-in Redis replacement, BSD-3
  ports: ["6379:6379"]
```

Spring Boot's `spring-boot-starter-data-redis` works with Valkey without any code changes.

### 1.3 Production Cost Estimate (Cloud)

| Component                    | Cost Estimate (per region)           | Notes                                    |
|------------------------------|--------------------------------------|------------------------------------------|
| OpenAI API (gpt-4o)         | ~$0.005/1K tokens (input)            | Pay per use; only production             |
| OpenAI Embedding API         | ~$0.00002/1K tokens                  | Very low cost                            |
| Managed PostgreSQL (RDS)     | ~$200–$500/month (db.r6g.large)      | Per region                               |
| Managed Redis / Valkey       | ~$50–$100/month                      | Per region                               |
| Managed Kafka (MSK)          | ~$200–$400/month                     | Per region; or use SQS/SNS as cheaper alternative |
| S3 storage                   | ~$23/TB/month                        | Negligible for CVs/letters               |
| Kubernetes (EKS)             | ~$73/month (control plane) + nodes   | Or use ECS Fargate for simpler ops       |
| Domain + DNS + TLS           | ~$15/year + free (Let's Encrypt)     |                                          |

**Local development total cost: $0**

---

## 2. Architecture Review — Multi-Tenancy

### 2.1 Current Design Assessment ✅

| Principle                       | Status | Implementation                                    |
|---------------------------------|--------|---------------------------------------------------|
| Tenant isolation                | ✅     | `tenant_id` column + PostgreSQL RLS on all app tables |
| Cross-tenant data leak prevention | ✅   | RLS policies + `TenantContextFilter` sets session variable per request |
| Tenant ID propagation           | ✅     | JWT claim `tenant_id` → extracted by filter → set as PostgreSQL session var |
| Multi-tenant vector store       | ✅     | `filterExpression` on `tenant_id` in metadata (application-layer) |
| Regional data isolation         | ✅     | Database-per-region; JWT `region` claim routes to correct cluster |
| Multi-tenant S3                 | ✅     | Path-based isolation: `/{region}/{tenant_id}/cv/{file_id}` |

### 2.2 Gaps Found & Fixes

#### Gap 1: No tenant_id validation on write operations
**Risk:** A compromised JWT could write data to another tenant.  
**Fix:** Add `@PrePersist` JPA entity listener that validates the entity's `tenant_id` matches the current security context.

#### Gap 2: Kafka events don't carry tenant context
**Risk:** Async consumers might process events without tenant scoping.  
**Fix:** Include `tenant_id` in every Kafka message header/payload; consumer sets tenant context before processing.

#### Gap 3: No tenant quota / rate limiting
**Risk:** One tenant could exhaust shared resources (AI calls, job searches).  
**Fix:** Per-tenant rate limits in Redis using `tenant_id` as the key prefix.

---

## 3. Architecture Review — Scalability

### 3.1 Current Design Assessment ✅

| Principle                    | Status | Implementation                                       |
|------------------------------|--------|------------------------------------------------------|
| Stateless services           | ✅     | JWT-based auth; no server-side sessions              |
| Horizontal scaling           | ✅     | HPA in K8s; multiple replicas behind LB              |
| Database connection pooling  | ✅     | HikariCP (default Spring Boot)                       |
| Async processing             | ✅     | Kafka for application submission, status events       |
| Caching                      | ✅     | Redis for job search results, rate limits             |
| Virtual threads              | ✅     | Java 25 virtual threads for I/O-heavy AI calls       |

### 3.2 Gaps Found & Fixes

#### Gap 4: No database read replica strategy documented
**Fix:** Add read replicas for analytics queries (UC-06 stats); Spring `@Transactional(readOnly = true)` routes to replica.

#### Gap 5: No connection pool sizing guidance for AI-heavy workloads
**Fix:** With virtual threads, each AI call holds a DB connection while waiting. Recommend: `maximumPoolSize = 50` per instance; use `spring.datasource.hikari.maximum-pool-size=50`.

#### Gap 6: Ollama embedding dimension mismatch between environments
**Risk:** Local dev uses 768d (nomic-embed-text), prod uses 1536d (OpenAI). Vectors are incompatible.  
**Fix:** Standardise on a configurable dimension. Use `${EMBEDDING_DIMENSION:768}` in Flyway migration and PgVectorStore config. Document that switching models requires re-embedding.

---

## 4. Architecture Review — Reliability

### 4.1 Current Design Assessment ✅

| Principle                    | Status | Implementation                                       |
|------------------------------|--------|------------------------------------------------------|
| Circuit breaker on AI calls  | ✅     | Documented in UC-03 error handling                   |
| Idempotent endpoints         | ✅     | UNIQUE constraint on `(user_id, job_listing_id)` for applications |
| Event sourcing for apps      | ✅     | `application_events` table records all state transitions |
| Retry with backoff           | ✅     | UC-05 retry strategy for failed applications          |
| Data encryption at rest      | ✅     | AES-256-GCM column encryption + S3 SSE                |

### 4.2 Gaps Found & Fixes

#### Gap 7: No circuit breaker library specified
**Fix:** Use **Resilience4j** (Apache 2.0, free). Spring Boot 4 has native support via `spring-cloud-circuitbreaker-resilience4j`.

#### Gap 8: No dead letter queue for failed Kafka messages
**Fix:** Configure Kafka DLT (Dead Letter Topic) for messages that fail after max retries. Spring Kafka's `@RetryableTopic` with `@DltHandler`.

#### Gap 9: No health check / liveness / readiness probes
**Fix:** Spring Boot Actuator endpoints:
- `/actuator/health/liveness` — app is running
- `/actuator/health/readiness` — app can accept traffic (DB connected, Kafka connected)
- K8s probes configured in Helm chart

#### Gap 10: No backup strategy for vector_store (RAG) data
**Fix:** `vector_store` is in the same PostgreSQL instance → covered by existing backup strategy. Add note to `07-DATA-MODEL.md`.

---

## 5. Architecture Review — Maintainability

### 5.1 Current Design Assessment ✅

| Principle                     | Status | Implementation                                    |
|-------------------------------|--------|---------------------------------------------------|
| Modular monolith              | ✅     | Clear package boundaries per use case             |
| Use-case-driven packages      | ✅     | auth/, cv/, jobsearch/, motivation/, apply/, application/ |
| Database migrations           | ✅     | Flyway with versioned SQL files                   |
| API versioning                | ✅     | URL versioning `/api/v1/`                         |
| Separation of concerns        | ✅     | Controller → Service → Repository → Model         |
| AI abstraction                | ✅     | Spring AI ChatClient; no provider coupling         |

### 5.2 Gaps Found & Fixes

#### Gap 11: No API deprecation strategy
**Fix:** When `/api/v2/` is introduced, keep `/api/v1/` running for 6 months with `Deprecation` header. Document in `00-SYSTEM-ARCHITECTURE.md`.

#### Gap 12: No structured logging / correlation IDs
**Fix:** Use **MDC** (Mapped Diagnostic Context) with `traceId`, `tenantId`, `userId` in every log line. OpenTelemetry auto-instrumentation propagates trace IDs across Kafka.

#### Gap 13: No module dependency rules
**Fix:** Add ArchUnit tests to enforce: `cv` module cannot depend on `motivation` module; `common` is the only shared dependency. Prevents spaghetti as the codebase grows.

---

## 6. Architecture Review — Future-Proofing

### 6.1 Current Strengths ✅

| Aspect                        | Future-Proof?  | Why                                              |
|-------------------------------|----------------|--------------------------------------------------|
| Spring AI abstraction         | ✅ Excellent    | Swap any LLM provider without code changes       |
| PgVectorStore for RAG         | ✅ Excellent    | Add new `doc_type` (company knowledge, Q&A) without schema changes |
| Modular monolith              | ✅ Good         | Can extract modules to microservices later        |
| Event-driven (Kafka)          | ✅ Excellent    | New consumers can subscribe without changing producers |
| Database-per-region           | ✅ Good         | Add new regions by deploying new cluster          |
| Docker Compose local          | ✅ Excellent    | New developer onboarding: `docker compose up`     |

### 6.2 Gaps Found & Fixes

#### Gap 14: No event schema evolution strategy
**Fix:** Use a schema registry (e.g., Confluent Schema Registry, Apache 2.0) or at minimum document event JSON schema versions. Use additive-only changes (new optional fields).

#### Gap 15: No feature flag system
**Fix:** Add **OpenFeature** (CNCF standard, Apache 2.0) with **Flagd** (open source) as the backend. Enables gradual rollouts (e.g., enable RAG company knowledge for 10% of users first).

#### Gap 16: No webhook / notification system for external integrations
**Fix:** Add a `webhooks` table where tenants configure callback URLs. Publish events to their endpoints on status changes. Required for enterprise customers.

#### Gap 17: Embedding dimension is hardcoded to 1536
**Fix:** Make dimension configurable via `${app.embedding.dimensions:768}`. Use this in Flyway migration and PgVectorStore config. When switching models, run a re-embedding job.

---

## 7. Summary of All Gaps & Fixes

| #  | Gap                                         | Severity | Sprint   | Fix                                                    |
|----|---------------------------------------------|----------|----------|--------------------------------------------------------|
| 1  | No tenant_id validation on writes           | High     | Sprint 0 | `@PrePersist` entity listener validates tenant match   |
| 2  | Kafka events missing tenant context         | High     | Sprint 0 | Include `tenant_id` in every Kafka message             |
| 3  | No per-tenant rate limiting                 | Medium   | Sprint 0 | Redis rate limits keyed by `tenant_id`                 |
| 4  | No read replica strategy                    | Low      | Sprint 6 | `@Transactional(readOnly=true)` routes to replica      |
| 5  | No connection pool sizing for AI workloads  | Medium   | Sprint 0 | Set `hikari.maximum-pool-size=50`                      |
| 6  | Embedding dimension mismatch local/prod     | High     | Sprint 0 | Configurable `${app.embedding.dimensions}` everywhere  |
| 7  | No circuit breaker library                  | Medium   | Sprint 0 | Add Resilience4j dependency                            |
| 8  | No Kafka dead letter queue                  | Medium   | Sprint 5 | `@RetryableTopic` + `@DltHandler`                      |
| 9  | No health/liveness/readiness probes         | Medium   | Sprint 0 | Spring Boot Actuator + K8s probe config                |
| 10 | vector_store backup not documented          | Low      | Sprint 0 | Add note — same PostgreSQL, same backup                |
| 11 | No API deprecation strategy                 | Low      | Sprint 6 | Document v1 → v2 migration path                       |
| 12 | No structured logging / correlation IDs     | Medium   | Sprint 0 | MDC with traceId, tenantId, userId                     |
| 13 | No module dependency rules (ArchUnit)       | Low      | Sprint 1 | Add ArchUnit test enforcing module boundaries          |
| 14 | No event schema evolution                   | Low      | Sprint 5 | Document event schemas; additive-only changes          |
| 15 | No feature flag system                      | Low      | Sprint 6 | OpenFeature + Flagd                                    |
| 16 | No webhook system for tenants               | Low      | v2       | `webhooks` table + event dispatch                      |
| 17 | Embedding dimension hardcoded               | High     | Sprint 0 | Configurable property in Flyway + PgVectorStore        |

---

## 8. Updated Technology Stack (Post-Audit)

| Layer              | Technology                                            | License      | Cost (Local) |
|--------------------|-------------------------------------------------------|-------------|-------------|
| Language           | Java 25 (Eclipse Temurin)                             | GPL v2+CPE  | Free        |
| Framework          | Spring Boot 4, Spring AI 1.x                         | Apache 2.0  | Free        |
| Frontend           | Vue.js 3, TypeScript, Vite, PrimeVue, Tailwind       | MIT         | Free        |
| AI (local)         | Ollama + Mistral 7B + nomic-embed-text               | MIT/Apache  | Free        |
| AI (cloud)         | OpenAI / Anthropic                                    | Commercial  | Pay-per-use |
| Database           | PostgreSQL 17 + pgvector                              | PostgreSQL/MIT | Free     |
| Vector Store / RAG | Spring AI PgVectorStore                               | Apache 2.0  | Free        |
| Caching            | **Valkey 8** (Redis-compatible, fully open source)    | BSD-3       | Free        |
| Messaging          | Apache Kafka                                          | Apache 2.0  | Free        |
| Resilience         | **Resilience4j**                                      | Apache 2.0  | Free        |
| Feature Flags      | **OpenFeature + Flagd**                               | Apache 2.0  | Free        |
| Auth               | Embedded Spring Authorization Server                  | Apache 2.0  | Free        |
| File Storage       | MinIO (S3-compatible, local) / AWS S3 (prod)          | AGPL / Commercial | Free (local) |
| Observability      | Micrometer + Prometheus + Grafana + OpenTelemetry     | Apache 2.0  | Free        |
| PDF Generation     | OpenPDF                                               | LGPL/MPL    | Free        |
| Text Extraction    | Apache Tika                                           | Apache 2.0  | Free        |
| Testing            | JUnit 5, Testcontainers, Vitest, Playwright           | Various OSS | Free        |
| CI/CD              | GitHub Actions                                        | Free tier   | Free (public repos) / $4/min (private) |
| Container Runtime  | Docker, Docker Compose                                | Apache 2.0  | Free        |
| Orchestration      | Kubernetes + Helm                                     | Apache 2.0  | Free        |

### **Total cost to run locally: $0.00**

Every component in the local development stack is open source with permissive licenses. No API keys, no SaaS subscriptions, no license fees required.

---

## 9. Local Development S3 Alternative: MinIO

The Docker Compose uses "S3-compatible object store" but doesn't specify a local alternative. **MinIO** (AGPL for server, Apache 2.0 for client) is the standard open-source S3 replacement for local dev:

```yaml
# Add to docker-compose.yml
minio:
  image: minio/minio:latest
  command: server /data --console-address ":9001"
  environment:
    MINIO_ROOT_USER: minioadmin
    MINIO_ROOT_PASSWORD: minioadmin
  ports:
    - "9000:9000"    # S3 API
    - "9001:9001"    # Web console
  volumes:
    - minio_data:/data
```

Spring Boot config for local:
```yaml
# application-local.yml
app:
  storage:
    type: s3
    endpoint: http://localhost:9000
    access-key: minioadmin
    secret-key: minioadmin
    bucket: jobagent-cv
    region: us-east-1
```

---

## 10. Revised Docker Compose (Complete — Post-Audit)

```yaml
# docker-compose.yml — all open source, zero cost
services:
  postgres:
    image: pgvector/pgvector:pg17
    environment:
      POSTGRES_DB: jobagent
      POSTGRES_USER: jobagent
      POSTGRES_PASSWORD: secret
    ports: ["5432:5432"]
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U jobagent"]
      interval: 5s
      timeout: 5s
      retries: 5

  valkey:
    image: valkey/valkey:8-alpine       # Fully open-source Redis replacement
    ports: ["6379:6379"]
    healthcheck:
      test: ["CMD", "valkey-cli", "ping"]
      interval: 5s

  kafka:
    image: bitnami/kafka:3.7
    environment:
      KAFKA_CFG_NODE_ID: 0
      KAFKA_CFG_PROCESS_ROLES: controller,broker
      KAFKA_CFG_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_CFG_CONTROLLER_QUORUM_VOTERS: 0@kafka:9093
      KAFKA_CFG_CONTROLLER_LISTENER_NAMES: CONTROLLER
    ports: ["9092:9092"]

  ollama:
    image: ollama/ollama:latest
    ports: ["11434:11434"]
    volumes:
      - ollama_data:/root/.ollama

  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio_data:/data

  jobagent-app:
    build: .
    depends_on:
      postgres:
        condition: service_healthy
      valkey:
        condition: service_healthy
      kafka:
        condition: service_started
      ollama:
        condition: service_started
      minio:
        condition: service_started
    environment:
      SPRING_PROFILES_ACTIVE: local
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/jobagent
      SPRING_DATASOURCE_USERNAME: jobagent
      SPRING_DATASOURCE_PASSWORD: secret
      SPRING_DATA_REDIS_HOST: valkey
      SPRING_AI_OLLAMA_BASE_URL: http://ollama:11434
      SPRING_AI_OLLAMA_CHAT_MODEL: mistral
      SPRING_AI_OLLAMA_EMBEDDING_MODEL: nomic-embed-text
      APP_STORAGE_ENDPOINT: http://minio:9000
      APP_STORAGE_ACCESS_KEY: minioadmin
      APP_STORAGE_SECRET_KEY: minioadmin
      APP_EMBEDDING_DIMENSIONS: 768
    ports: ["8080:8080"]

  jobagent-ui:
    build: ./jobagent-ui
    depends_on: [jobagent-app]
    ports: ["5173:80"]

volumes:
  postgres_data:
  ollama_data:
  minio_data:
```

---

## 11. ADR Summary (New Decisions from Audit)

| #       | Decision                                     | Rationale                                         |
|---------|----------------------------------------------|---------------------------------------------------|
| ADR-014 | **Valkey** replaces Redis                    | Fully BSD-3; drop-in Redis replacement; avoids SSPL licensing risk |
| ADR-015 | **Resilience4j** for circuit breaking        | Apache 2.0; native Spring Boot support; retry + circuit breaker + rate limiter |
| ADR-016 | **MinIO** for local S3                       | Free, S3-compatible, web console; no AWS account needed for local dev |
| ADR-017 | **Configurable embedding dimensions**         | Avoid mismatch between Ollama (768d) and OpenAI (1536d); single property controls Flyway + PgVectorStore |
| ADR-018 | **OpenFeature + Flagd** for feature flags    | CNCF standard; open source; enables gradual rollout of features like RAG company knowledge |
| ADR-019 | **ArchUnit** for module dependency enforcement | Prevent cross-module coupling as codebase grows |
| ADR-020 | **MDC structured logging** with correlation IDs | traceId, tenantId, userId in every log line for production debugging |
