# 00 — System Architecture Overview

## 1. Vision

**JobAgent** is a global, multi-tenant, AI-powered job application platform.  
It helps job seekers discover matching positions, generate tailored motivation letters, and apply automatically — all driven by Spring AI agents.

---

## 2. Key Quality Attributes

| Attribute        | Strategy                                                                                   |
|------------------|--------------------------------------------------------------------------------------------|
| **Scalability**  | Stateless services behind a load balancer; horizontal pod auto-scaling in Kubernetes; virtual threads for I/O-heavy AI calls. |
| **Reliability**  | Resilience4j circuit-breaker + retry on AI calls; idempotent endpoints; Kafka DLT for failed messages; event sourcing for applications. |
| **Maintainability** | Modular monolith (one deployable, clear package boundaries); ArchUnit module dependency rules; use-case-driven packages. |
| **Testability**  | Docker Compose (all open-source: Postgres, Valkey, Ollama, MinIO, Kafka); Testcontainers in CI. |
| **Data Residency** | Region-aware tenant routing; each region runs its own Postgres cluster.                  |
| **Security**     | OAuth 2.1 / OpenID Connect; encryption at rest (AES-256) and in transit (TLS 1.3).        |
| **Observability** | Structured logging (MDC: traceId, tenantId, userId); Micrometer metrics; OpenTelemetry tracing; health/readiness probes. |

---

## 3. Technology Stack

| Layer            | Technology                                                      |
|------------------|-----------------------------------------------------------------|
| Language         | Java 25 (virtual threads enabled)                               |
| Framework        | Spring Boot 4, Spring AI 1.x                                   |
| AI Orchestration | Spring AI (ChatClient, function calling, prompt templates)      |
| AI Provider (local) | Ollama (lightweight, open-source — e.g., Mistral, Llama 3)  |
| AI Provider (cloud) | OpenAI / Anthropic (production)                              |
| Database         | PostgreSQL 17 (one cluster per region)                          |
| Vector Store / RAG | pgvector + Spring AI PgVectorStore (HNSW index, RAG retrieval) |
| Caching          | Valkey 8 (open-source Redis fork, BSD-3) — session, rate-limit, job cache |
| Messaging        | Apache Kafka / Spring Cloud Stream (async events)               |
| Storage          | MinIO (local, S3-compatible) / AWS S3 (production)              |
| Auth             | Embedded Spring Authorization Server (OAuth 2.1 / OpenID Connect) |
| Containerisation | Docker, Docker Compose (local), Kubernetes + Helm (cloud)       |
| Resilience       | Resilience4j (circuit breaker, retry, rate limiter)             |
| Feature Flags    | OpenFeature + Flagd (CNCF standard, open source)               |
| CI/CD            | GitHub Actions → Docker image → Helm deploy                    |
| Observability    | Micrometer → Prometheus + Grafana; OpenTelemetry tracing        |

---

## 4. High-Level Component Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                     Vue.js SPA (jobagent-ui)                         │
│          Nginx / Vite dev server — port 5173 (local)                 │
│   (Auth via PKCE → Spring Auth Server, API calls via Axios)          │
└──────────────┬───────────────────────────────────────────────────────┘
               │
┌──────────────▼───────────────────────────────────────────────────────┐
│                          API Gateway / LB                            │
│                  (rate limiting, JWT validation)                      │
└──────────────┬──────────────────────────────────┬────────────────────┘
               │                                  │
     ┌─────────▼──────────┐            ┌──────────▼─────────────┐
     │  JobAgent Service   │            │  JobAgent Service       │
     │  (Region EU)        │            │  (Region US)            │
     │  ┌───────────────┐  │            │  ┌───────────────┐      │
     │  │ Auth Module    │  │            │  │ Auth Module    │      │
     │  │ CV Module      │  │            │  │ CV Module      │      │
     │  │ Job Search Mod │  │            │  │ Job Search Mod │      │
     │  │ Motivation Mod │  │            │  │ Motivation Mod │      │
     │  │ Apply Module   │  │            │  │ Apply Module   │      │
     │  │ App View Mod   │  │            │  │ App View Mod   │      │
     │  └───────────────┘  │            │  └───────────────┘      │
     │         │            │            │         │                │
     │  ┌──────▼────────┐  │            │  ┌──────▼────────┐      │
     │  │ PostgreSQL EU  │  │            │  │ PostgreSQL US  │      │
     │  │ S3 / MinIO EU  │  │            │  │ S3 / MinIO US  │      │
     │  │ Valkey EU      │  │            │  │ Valkey US      │      │
     │  └───────────────┘  │            │  └───────────────┘      │
     └─────────────────────┘            └──────────────────────────┘
               │                                  │
               └──────────┬───────────────────────┘
                          │
                ┌─────────▼──────────┐
                │   Kafka Cluster     │
                │   (cross-region     │
                │    event bus)       │
                └─────────┬──────────┘
                          │
                ┌─────────▼──────────┐
                │  AI Provider(s)     │
                │  Local: Ollama      │
                │  Cloud: OpenAI /    │
                │         Anthropic   │
                │  (via Spring AI)    │
                └────────────────────┘
```

---

## 5. Multi-Tenancy & Data Residency

### 5.1 Tenant Isolation Model

* **Tenant = Organisation or Individual User** — each tenant belongs to exactly one **region**.
* A **region** (EU, US, APAC, …) maps to an isolated infrastructure set (Postgres, S3, Redis).
* The API Gateway inspects the JWT `region` claim and routes to the correct regional deployment.

### 5.2 Database-per-Region Strategy

```
Region EU  →  postgres-eu.jobagent.internal   (schema: public)
Region US  →  postgres-us.jobagent.internal   (schema: public)
Region APAC → postgres-apac.jobagent.internal (schema: public)
```

Within each regional Postgres cluster, tenant isolation is achieved via a `tenant_id` column on every table plus Row-Level Security (RLS) policies.

### 5.3 Data Residency Compliance

| Regulation | Region | Controls                                                |
|------------|--------|---------------------------------------------------------|
| GDPR       | EU     | Data stays in EU cluster; right-to-erasure API; DPA.    |
| CCPA       | US     | Data stays in US cluster; opt-out support.              |
| PDPA       | APAC   | Data stays in APAC cluster.                             |

---

## 6. Personal Data & CV Storage Strategy

### 6.1 What We Store

| Data Category         | Storage Location              | Encryption                      |
|-----------------------|-------------------------------|---------------------------------|
| User credentials      | PostgreSQL `users` table      | Password: bcrypt; email: AES-256 column encryption |
| Profile metadata      | PostgreSQL `user_profiles`    | AES-256 column encryption for PII fields           |
| Raw CV file (PDF/DOCX)| S3-compatible object store    | SSE-S3 (server-side encryption)                    |
| Parsed CV (structured)| PostgreSQL `cv_details`       | AES-256 column encryption for sensitive fields     |
| CV embeddings (vector)| PostgreSQL pgvector extension | Not PII — derived feature vectors                  |

### 6.2 Encryption at Rest

* **Column-level encryption** using Spring's `@Convert` with a custom `AttributeConverter<String, String>` that delegates to AES-256-GCM.
* The encryption key is stored in a cloud KMS (AWS KMS / Azure Key Vault / HashiCorp Vault) — never in the database.
* In local/Docker mode a symmetric key from `application-local.properties` is used (test-only).

### 6.3 CV Processing Pipeline

```
User uploads CV (PDF / DOCX)
        │
        ▼
┌─────────────────────────┐
│    CVAnalyzerAgent       │  (Spring AI function-calling agent)
│  1. Store raw file       │──► S3 bucket (region-local)
│  2. Extract text         │   (Apache Tika)
│  3. AI parse             │──► LLM extracts structured fields
│  4. Generate embedding   │──► pgvector cv_embeddings (for job ranking)
│  5. Chunk by section     │──► Split into SKILLS, EXPERIENCE, EDUCATION, etc.
│  6. Ingest into          │──► PgVectorStore vector_store table (for RAG)
│     PgVectorStore        │    with metadata: tenant_id, cv_id, section, doc_type
│  7. Persist cv_details   │──► PostgreSQL
└─────────────────────────┘
```

### 6.4 Access Control

* Every query is scoped by `tenant_id` (enforced at repository layer **and** PostgreSQL RLS).
* CV files in S3 are stored under path `/{region}/{tenant_id}/cv/{file_id}` — IAM policy restricts cross-tenant access.
* Signed URLs with 5-minute TTL are used for CV downloads.

### 6.5 Right-to-Erasure (GDPR Article 17)

A dedicated `DELETE /api/v1/users/{id}/data` endpoint triggers:
1. Delete S3 objects for tenant.
2. Delete RAG chunks from `vector_store` where `metadata->>'tenant_id' = ?`.
3. Hard-delete rows in `cv_details`, `cv_embeddings`, `applications`, `motivation_letters`, `user_profiles`, `users`.
4. Publish `UserDataErased` event to Kafka for audit trail (event contains only tenant_id, timestamp — no PII).

---

## 7. Package Structure (Modular Monolith)

```
com.jobagent.jobagent
├── auth/              ← UC-01  Register / Login + Spring Authorization Server
│   ├── config/        ← AuthorizationServerConfig, SecurityFilterChain, TokenCustomizer
│   ├── federation/    ← FederatedIdentityUserHandler, OAuth2UserService
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── model/
│   └── dto/
├── cv/                ← UC-02  Upload & Analyse CV
│   ├── controller/
│   ├── service/
│   ├── agent/         ← CVAnalyzerAgent (Spring AI)
│   ├── repository/
│   ├── model/
│   └── dto/
├── jobsearch/         ← UC-03  Search Jobs
│   ├── controller/
│   ├── service/
│   ├── agent/         ← JobFinderAgent
│   ├── repository/
│   ├── model/
│   └── dto/
├── motivation/        ← UC-04  Generate Motivation Letter
│   ├── controller/
│   ├── service/
│   ├── agent/         ← MotivationWriterAgent
│   ├── repository/
│   ├── model/
│   └── dto/
├── apply/             ← UC-05  Apply to Job
│   ├── controller/
│   ├── service/
│   ├── agent/         ← ApplyAgent
│   ├── repository/
│   ├── model/
│   └── dto/
├── application/       ← UC-06  View Applications
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── model/
│   └── dto/
├── common/            ← Cross-cutting: encryption, tenant context, RAG config, exceptions
│   ├── security/
│   ├── multitenancy/
│   ├── rag/           ← PgVectorStore config, VectorStore bean, shared RAG utilities
│   ├── encryption/
│   └── config/
└── JavaAgentApplication.java
```

---

## 8. Local Development (Docker Compose)

```yaml
# docker-compose.yml  (simplified)
services:
  postgres:
    image: pgvector/pgvector:pg17
    environment:
      POSTGRES_DB: jobagent
      POSTGRES_USER: jobagent
      POSTGRES_PASSWORD: secret
    ports: ["5432:5432"]
    volumes: [postgres_data:/var/lib/postgresql/data]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U jobagent"]
      interval: 5s

  valkey:
    image: valkey/valkey:8-alpine          # Fully open-source Redis replacement (BSD-3)
    ports: ["6379:6379"]

  kafka:
    image: bitnami/kafka:3.7
    ports: ["9092:9092"]

  ollama:
    image: ollama/ollama:latest
    ports: ["11434:11434"]
    volumes: [ollama_data:/root/.ollama]

  minio:
    image: minio/minio:latest              # Open-source S3-compatible storage
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports: ["9000:9000", "9001:9001"]
    volumes: [minio_data:/data]

  jobagent-app:
    build: .
    depends_on: [postgres, valkey, kafka, ollama, minio]
    environment:
      SPRING_PROFILES_ACTIVE: local
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/jobagent
      SPRING_DATA_REDIS_HOST: valkey
      SPRING_AI_OLLAMA_BASE_URL: http://ollama:11434
      SPRING_AI_OLLAMA_CHAT_MODEL: mistral
      SPRING_AI_OLLAMA_EMBEDDING_MODEL: nomic-embed-text
      APP_STORAGE_ENDPOINT: http://minio:9000
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

### 8.1 AI Provider Strategy (Ollama Local / OpenAI Cloud)

Spring AI's abstraction (`ChatClient`, `EmbeddingModel`) lets us swap providers via **Spring profiles** with zero code changes.

| Profile  | Chat Model           | Embedding Model        | Provider | Use Case              |
|----------|----------------------|------------------------|----------|-----------------------|
| `local`  | `mistral` (7B)       | `nomic-embed-text`     | Ollama   | Development & testing |
| `prod`   | `gpt-4o` / `claude`  | `text-embedding-3-small` | OpenAI / Anthropic | Production |

**`application-local.yml`**
```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: mistral
          temperature: 0.7
      embedding:
        options:
          model: nomic-embed-text
```

**`application-prod.yml`**
```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o
          temperature: 0.7
      embedding:
        options:
          model: text-embedding-3-small
```

**How it works:** Spring AI auto-configures the correct `ChatModel` and `EmbeddingModel` beans based on which starter is on the classpath and which profile properties are set. All agents (`CVAnalyzerAgent`, `JobFinderAgent`, `MotivationWriterAgent`, `ApplyAgent`) inject `ChatClient` — they never reference a specific provider.

**Ollama first-time setup:**
```bash
# Pull models (one-time, ~4GB for mistral + ~300MB for nomic-embed-text)
docker exec ollama ollama pull mistral
docker exec ollama ollama pull nomic-embed-text
```

**Recommended local models:**

| Model                | Size  | Purpose            | Notes                               |
|----------------------|-------|--------------------|-------------------------------------|
| `mistral` (7B)       | ~4 GB | Chat / generation  | Good quality, fast on Apple Silicon |
| `llama3.1` (8B)      | ~4.7 GB | Chat / generation | Better reasoning, slightly slower   |
| `nomic-embed-text`   | ~300 MB | Embeddings (768d) | Lightweight; dimension differs from OpenAI (1536d) — use `pgvector` with matching dimension |
| `mxbai-embed-large`  | ~670 MB | Embeddings (1024d)| Higher quality embeddings           |

> **Note:** Embedding dimensions differ between Ollama and OpenAI models. The dimension is **configurable** via `${APP_EMBEDDING_DIMENSIONS:768}`:
> - Local (Ollama `nomic-embed-text`): 768d
> - Production (OpenAI `text-embedding-3-small`): 1536d
>
> The Flyway migration uses `vector(${dimensions})` and PgVectorStore is configured with the same property. **Switching models requires re-embedding all existing vectors.**

---

## 9. Cloud Deployment (Kubernetes)

Each **region** has its own Kubernetes namespace:

```
k8s-cluster-eu
  └── namespace: jobagent-eu
       ├── Deployment: jobagent-app (replicas: 2–10, HPA)
       ├── Deployment: jobagent-ui  (replicas: 2–4, Nginx)
       ├── StatefulSet: postgres-eu
       ├── Deployment: valkey-eu
       ├── Ingress: api-eu.jobagent.com    (→ jobagent-app)
       └── Ingress: app-eu.jobagent.com    (→ jobagent-ui)

k8s-cluster-us
  └── namespace: jobagent-us
       └── (mirror)
```

Global DNS (Route 53 / Cloudflare) with geo-routing sends users to the nearest region.

---

## 10. Use Case Index

| # | Use Case                  | Document                          |
|---|---------------------------|-----------------------------------|
| 1 | Register / Login          | `01-UC-REGISTER-LOGIN.md`         |
| 2 | Upload CV                 | `02-UC-UPLOAD-CV.md`              |
| 3 | Search Jobs               | `03-UC-SEARCH-JOBS.md`            |
| 4 | Generate Motivation Letter| `04-UC-GENERATE-MOTIVATION.md`    |
| 5 | Apply to Job              | `05-UC-APPLY-JOB.md`             |
| 6 | View Applications         | `06-UC-VIEW-APPLICATIONS.md`     |
| 7 | Data Model                | `07-DATA-MODEL.md`               |
| 8 | UI Module (Vue.js SPA)    | `08-UC-UI-MODULE.md`             |
| 9 | Design Review & RAG Analysis | `09-DESIGN-REVIEW-RAG-ANALYSIS.md` |
| 10 | Design Audit: Licensing & Production Readiness | `10-DESIGN-AUDIT-LICENSING.md` |
| 11 | **Complete Architecture Diagram (All Components)** | `11-COMPLETE-ARCHITECTURE-DIAGRAM.md` |
