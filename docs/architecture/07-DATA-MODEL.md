# 07 — Data Model (Complete)

## 1. Overview

This document consolidates the complete PostgreSQL data model for JobAgent.  
All tables live within a single regional PostgreSQL cluster (database per region).  
Tenant isolation is enforced via `tenant_id` column + Row-Level Security (RLS).

---

## 2. Entity Relationship Diagram

```
┌──────────────┐       ┌──────────────────┐       ┌──────────────────┐
│    users     │       │  user_profiles   │       │   cv_details     │
│──────────────│       │──────────────────│       │──────────────────│
│ id (PK)      │◄──────│ user_id (FK)     │       │ id (PK)          │
│ tenant_id    │       │ id (PK)          │       │ user_id (FK) ────│──► users.id
│ email_encrypt│       │ tenant_id        │       │ tenant_id        │
│ email_hash   │       │ phone_encrypted  │       │ file_name        │
│ password_hash│       │ address_encrypted│       │ s3_key           │
│ full_name    │       │ linkedin_url     │       │ parsed_json      │
│ country      │       │ preferred_titles │       │ status           │
│ region       │       │ preferred_locs   │       │ ...              │
│ auth_provider│       │ ...              │       └────────┬─────────┘
│ ...          │       └──────────────────┘                │
└──────┬───────┘                                           │
       │                                          ┌────────▼─────────┐
       │                                          │  cv_embeddings   │
       │                                          │──────────────────│
       │                                          │ id (PK)          │
       │                                          │ cv_id (FK) ──────│──► cv_details.id
       │                                          │ tenant_id        │
       │                                          │ embedding (vec)  │
       │                                          └──────────────────┘
       │
       │                                          ┌──────────────────────┐
       │                                          │  vector_store (RAG)  │
       │                                          │──────────────────────│
       │                                          │ id (PK)              │
       │                                          │ content (TEXT)       │
       │                                          │ metadata (JSONB)     │
       │                                          │  ├ cv_id ────────────│──► cv_details.id
       │                                          │  ├ tenant_id         │
       │                                          │  ├ section           │
       │                                          │  └ doc_type          │
       │                                          │ embedding (vec)      │
       │                                          └──────────────────────┘
       │
       │         ┌──────────────────┐       ┌──────────────────┐
       │         │  job_listings    │       │  job_embeddings  │
       │         │──────────────────│       │──────────────────│
       │         │ id (PK)          │◄──────│ job_listing_id   │
       │         │ tenant_id        │       │ id (PK)          │
       │         │ title            │       │ tenant_id        │
       │         │ company          │       │ embedding (vec)  │
       │         │ location         │       └──────────────────┘
       │         │ description      │
       │         │ source           │       ┌──────────────────┐
       │         │ source_url       │       │  saved_jobs      │
       │         │ ...              │◄──────│ job_listing_id   │
       │         └────────┬─────────┘       │ user_id ─────────│──► users.id
       │                  │                 │ tenant_id        │
       │                  │                 └──────────────────┘
       │                  │
       │    ┌─────────────▼──────────────┐
       │    │   motivation_letters       │
       │    │────────────────────────────│
       │    │ id (PK)                    │
       ├────│ user_id (FK)               │
       │    │ tenant_id                  │
       │    │ cv_id (FK) ────────────────│──► cv_details.id
       │    │ job_listing_id (FK) ───────│──► job_listings.id
       │    │ letter_text_encrypted      │
       │    │ tone, language             │
       │    │ status, version            │
       │    │ pdf_s3_key                 │
       │    │ ...                        │
       │    └─────────────┬──────────────┘
       │                  │
       │    ┌─────────────▼──────────────┐
       │    │ motivation_letter_history  │
       │    │────────────────────────────│
       │    │ id (PK)                    │
       │    │ letter_id (FK)             │
       │    │ version                    │
       │    │ letter_text_encrypted      │
       │    │ generated_at               │
       │    └────────────────────────────┘
       │
       │    ┌────────────────────────────┐
       │    │      applications          │
       │    │────────────────────────────│
       │    │ id (PK)                    │
       ├────│ user_id (FK)               │
       │    │ tenant_id                  │
       │    │ job_listing_id (FK) ───────│──► job_listings.id
       │    │ cv_id (FK) ────────────────│──► cv_details.id
       │    │ letter_id (FK) ────────────│──► motivation_letters.id
       │    │ status                     │
       │    │ apply_method               │
       │    │ confirmation_ref           │
       │    │ failure_reason             │
       │    │ ...                        │
       │    └─────────────┬──────────────┘
       │                  │
       │    ┌─────────────▼──────────────┐
       │    │   application_events       │
       │    │────────────────────────────│
       │    │ id (PK)                    │
       │    │ application_id (FK)        │
       │    │ tenant_id                  │
       │    │ event_type                 │
       │    │ old_status, new_status     │
       │    │ details                    │
       │    │ created_at                 │
       │    └────────────────────────────┘
       │
       │    ┌────────────────────────────┐
       │    │   application_notes        │
       │    │────────────────────────────│
       │    │ id (PK)                    │
       ├────│ user_id (FK)               │
       │    │ application_id (FK)        │
       │    │ tenant_id                  │
       │    │ note_text                  │
       │    │ created_at                 │
       │    └────────────────────────────┘
       │
       │    ┌────────────────────────────┐
       │    │  job_source_configs        │
       │    │────────────────────────────│
       │    │ id (PK)                    │
       │    │ tenant_id                  │
       │    │ name                       │
       │    │ base_url                   │
       │    │ scraper_type               │
       │    │ auth_config (JSONB, enc)   │
       │    │ enabled                    │
       │    └────────────────────────────┘
       │
       │    ┌─────────────────────────────────┐
       │    │  oauth2_registered_client       │
       │    │─────────────────────────────────│
       │    │ id (PK)                         │
       │    │ client_id                       │
       │    │ client_name                     │
       │    │ authorization_grant_types       │
       │    │ scopes                          │
       │    │ ...                             │
       │    └─────────────────────────────────┘
       │
       │    ┌─────────────────────────────────┐
       │    │  oauth2_authorization           │
       │    │─────────────────────────────────│
       │    │ id (PK)                         │
       │    │ registered_client_id            │
       │    │ principal_name                  │
       │    │ authorization_grant_type        │
       │    │ access/refresh/id tokens        │
       │    │ ...                             │
       │    └─────────────────────────────────┘
       │
       │    ┌─────────────────────────────────┐
       │    │  oauth2_authorization_consent   │
       │    │─────────────────────────────────│
       │    │ registered_client_id (PK)       │
       │    │ principal_name (PK)             │
       │    │ authorities                     │
       │    └─────────────────────────────────┘
```

---

## 3. Complete DDL

### 3.1 Extensions

```sql
-- Required extensions (run once per database)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";     -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "vector";       -- pgvector for embeddings
```

### 3.2 Users & Profiles

```sql
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL DEFAULT gen_random_uuid(),
    email_encrypted TEXT NOT NULL,
    email_hash      TEXT NOT NULL UNIQUE,
    password_hash   TEXT,
    full_name       TEXT NOT NULL,
    country         VARCHAR(2) NOT NULL,
    region          VARCHAR(10) NOT NULL,
    auth_provider   VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    enabled         BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_tenant ON users(tenant_id);
CREATE INDEX idx_users_email_hash ON users(email_hash);
CREATE INDEX idx_users_region ON users(region);

CREATE TABLE user_profiles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id           UUID NOT NULL,
    phone_encrypted     TEXT,
    address_encrypted   TEXT,
    linkedin_url        TEXT,
    preferred_job_titles TEXT[],
    preferred_locations  TEXT[],
    preferred_remote    BOOLEAN DEFAULT false,
    preferred_salary_min BIGINT,
    preferred_currency  VARCHAR(3),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_user_profiles_user ON user_profiles(user_id);
CREATE INDEX idx_user_profiles_tenant ON user_profiles(tenant_id);
```

### 3.3 CV & Embeddings

```sql
CREATE TABLE cv_details (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id           UUID NOT NULL,
    file_name           TEXT NOT NULL,
    s3_key              TEXT NOT NULL,
    content_type        VARCHAR(100) NOT NULL,
    file_size_bytes     BIGINT NOT NULL,
    extracted_text      TEXT,
    parsed_json         JSONB NOT NULL DEFAULT '{}',
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_cv_details_user ON cv_details(user_id, tenant_id);
CREATE INDEX idx_cv_details_status ON cv_details(user_id, status);

CREATE TABLE cv_embeddings (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cv_id       UUID NOT NULL REFERENCES cv_details(id) ON DELETE CASCADE,
    tenant_id   UUID NOT NULL,
    embedding   vector(${APP_EMBEDDING_DIMENSIONS}),   -- 768 (Ollama) or 1536 (OpenAI); set via Flyway placeholder
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_cv_embeddings_cv ON cv_embeddings(cv_id);
CREATE INDEX idx_cv_embeddings_vector ON cv_embeddings
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
```

### 3.3.1 PgVectorStore — RAG Knowledge Base (Spring AI Managed)

This table is **auto-created and managed by Spring AI's `PgVectorStore`**.  
It stores chunked documents (CV sections, company knowledge) for RAG retrieval.

```sql
-- Auto-created by Spring AI PgVectorStore
CREATE TABLE IF NOT EXISTS vector_store (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content   TEXT NOT NULL,                    -- chunk text
    metadata  JSONB NOT NULL DEFAULT '{}',      -- filterable metadata (see below)
    embedding vector(${APP_EMBEDDING_DIMENSIONS})   -- auto-generated by EmbeddingModel; 768 or 1536
);

CREATE INDEX idx_vector_store_embedding ON vector_store
    USING hnsw (embedding vector_cosine_ops);

-- Custom index for tenant-scoped RAG queries
CREATE INDEX idx_vector_store_metadata ON vector_store
    USING gin (metadata jsonb_path_ops);
```

**Metadata schema:**
```json
{
  "cv_id": "a1b2c3d4-...",
  "tenant_id": "x9y8z7-...",
  "section": "EXPERIENCE",
  "doc_type": "cv_chunk"
}
```

| `doc_type` value     | Description                                      | Populated by       |
|----------------------|--------------------------------------------------|--------------------|
| `cv_chunk`           | CV section chunks (skills, experience, education) | UC-02: Upload CV   |
| `company_knowledge`  | Company info (website, reviews, news) — v2        | Future: scraper    |

**Dual storage rationale:**

| Store            | Purpose                                       | Used By                          |
|------------------|-----------------------------------------------|----------------------------------|
| `cv_embeddings`  | Whole-CV vector for job **ranking** (cosine)   | UC-03: Job Search ranking        |
| `vector_store`   | Chunked sections for **RAG retrieval** (inject into LLM prompt) | UC-04: Motivation Letter, UC-03: Match explanation |

### 3.4 Job Listings & Search

```sql
CREATE TABLE job_listings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    external_id     TEXT,
    title           TEXT NOT NULL,
    company         TEXT NOT NULL,
    location        TEXT,
    remote          BOOLEAN NOT NULL DEFAULT false,
    salary_range    TEXT,
    description     TEXT NOT NULL,
    source          VARCHAR(50) NOT NULL,
    source_url      TEXT NOT NULL,
    application_url TEXT,
    posted_at       TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, source, external_id)
);

CREATE INDEX idx_job_listings_tenant ON job_listings(tenant_id);
CREATE INDEX idx_job_listings_source ON job_listings(source);
CREATE INDEX idx_job_listings_created ON job_listings(created_at DESC);

CREATE TABLE job_embeddings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_listing_id  UUID NOT NULL REFERENCES job_listings(id) ON DELETE CASCADE,
    tenant_id       UUID NOT NULL,
    embedding       vector(${APP_EMBEDDING_DIMENSIONS}),   -- 768 (Ollama) or 1536 (OpenAI)
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_job_embeddings_job ON job_embeddings(job_listing_id);
CREATE INDEX idx_job_embeddings_vector ON job_embeddings
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

CREATE TABLE saved_jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_listing_id  UUID NOT NULL REFERENCES job_listings(id) ON DELETE CASCADE,
    tenant_id       UUID NOT NULL,
    saved_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, job_listing_id)
);

CREATE TABLE job_source_configs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    name            VARCHAR(100) NOT NULL,
    base_url        TEXT NOT NULL,
    scraper_type    VARCHAR(30) NOT NULL,
    auth_config     JSONB,
    enabled         BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_job_source_configs_tenant ON job_source_configs(tenant_id, enabled);
```

### 3.5 Motivation Letters

```sql
CREATE TABLE motivation_letters (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id               UUID NOT NULL,
    cv_id                   UUID NOT NULL REFERENCES cv_details(id),
    job_listing_id          UUID NOT NULL REFERENCES job_listings(id),
    letter_text_encrypted   TEXT NOT NULL,
    tone                    VARCHAR(20) NOT NULL,
    language                VARCHAR(5) NOT NULL,
    additional_instructions TEXT,
    word_count              INT,
    status                  VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    pdf_s3_key              TEXT,
    version                 INT NOT NULL DEFAULT 1,
    generated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_motivation_letters_user ON motivation_letters(user_id, tenant_id);
CREATE INDEX idx_motivation_letters_job ON motivation_letters(job_listing_id);

CREATE TABLE motivation_letter_history (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    letter_id               UUID NOT NULL REFERENCES motivation_letters(id) ON DELETE CASCADE,
    version                 INT NOT NULL,
    letter_text_encrypted   TEXT NOT NULL,
    generated_at            TIMESTAMPTZ NOT NULL,
    UNIQUE (letter_id, version)
);
```

### 3.6 Applications & Events

```sql
CREATE TABLE applications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id           UUID NOT NULL,
    job_listing_id      UUID NOT NULL REFERENCES job_listings(id),
    cv_id               UUID NOT NULL REFERENCES cv_details(id),
    letter_id           UUID REFERENCES motivation_letters(id),
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    apply_method        VARCHAR(20),
    confirmation_ref    TEXT,
    failure_reason      TEXT,
    additional_message  TEXT,
    submitted_at        TIMESTAMPTZ,
    version             INT NOT NULL DEFAULT 0,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, job_listing_id)
);

CREATE INDEX idx_applications_user ON applications(user_id, tenant_id);
CREATE INDEX idx_applications_status ON applications(status);
CREATE INDEX idx_applications_submitted ON applications(submitted_at DESC);

CREATE TABLE application_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id  UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    tenant_id       UUID NOT NULL,
    event_type      VARCHAR(30) NOT NULL,
    old_status      VARCHAR(30),
    new_status      VARCHAR(30),
    details         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_application_events_app ON application_events(application_id);
CREATE INDEX idx_application_events_created ON application_events(created_at DESC);

CREATE TABLE application_notes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id  UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    tenant_id       UUID NOT NULL,
    user_id         UUID NOT NULL REFERENCES users(id),
    note_text       TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_application_notes_app ON application_notes(application_id, tenant_id);
```

### 3.7 Spring Authorization Server (OAuth2)

```sql
-- Standard schema required by Spring Authorization Server
-- These tables store registered OAuth2 clients, authorizations, and consent.

CREATE TABLE oauth2_registered_client (
    id                          VARCHAR(100) PRIMARY KEY,
    client_id                   VARCHAR(100) NOT NULL,
    client_id_issued_at         TIMESTAMPTZ DEFAULT now(),
    client_secret               VARCHAR(200),
    client_secret_expires_at    TIMESTAMPTZ,
    client_name                 VARCHAR(200) NOT NULL,
    client_authentication_methods VARCHAR(1000) NOT NULL,
    authorization_grant_types   VARCHAR(1000) NOT NULL,
    redirect_uris               VARCHAR(1000),
    post_logout_redirect_uris   VARCHAR(1000),
    scopes                      VARCHAR(1000) NOT NULL,
    client_settings             VARCHAR(2000) NOT NULL,
    token_settings              VARCHAR(2000) NOT NULL
);

CREATE TABLE oauth2_authorization (
    id                          VARCHAR(100) PRIMARY KEY,
    registered_client_id        VARCHAR(100) NOT NULL,
    principal_name              VARCHAR(200) NOT NULL,
    authorization_grant_type    VARCHAR(100) NOT NULL,
    authorized_scopes           VARCHAR(1000),
    attributes                  TEXT,
    state                       VARCHAR(500),
    authorization_code_value    TEXT,
    authorization_code_issued_at TIMESTAMPTZ,
    authorization_code_expires_at TIMESTAMPTZ,
    authorization_code_metadata TEXT,
    access_token_value          TEXT,
    access_token_issued_at      TIMESTAMPTZ,
    access_token_expires_at     TIMESTAMPTZ,
    access_token_metadata       TEXT,
    access_token_type           VARCHAR(100),
    access_token_scopes         VARCHAR(1000),
    oidc_id_token_value         TEXT,
    oidc_id_token_issued_at     TIMESTAMPTZ,
    oidc_id_token_expires_at    TIMESTAMPTZ,
    oidc_id_token_metadata      TEXT,
    refresh_token_value         TEXT,
    refresh_token_issued_at     TIMESTAMPTZ,
    refresh_token_expires_at    TIMESTAMPTZ,
    refresh_token_metadata      TEXT,
    user_code_value             TEXT,
    user_code_issued_at         TIMESTAMPTZ,
    user_code_expires_at        TIMESTAMPTZ,
    user_code_metadata          TEXT,
    device_code_value           TEXT,
    device_code_issued_at       TIMESTAMPTZ,
    device_code_expires_at      TIMESTAMPTZ,
    device_code_metadata        TEXT
);

CREATE TABLE oauth2_authorization_consent (
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name       VARCHAR(200) NOT NULL,
    authorities          VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);
```

---

## 4. Row-Level Security (RLS)

All application tables enforce tenant isolation via RLS:

```sql
-- Example for users table (repeat pattern for all application tables)
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_users ON users
    USING (tenant_id = current_setting('app.current_tenant')::uuid);

-- The application sets the tenant context per request:
-- SET LOCAL app.current_tenant = '<tenant-uuid>';
```

**Note on `vector_store` table:** The PgVectorStore table does **not** use RLS because it is managed by Spring AI.  
Tenant isolation is enforced at the **application layer** via `filterExpression` in every `SearchRequest`:

```java
SearchRequest.builder()
    .query(queryText)
    .topK(5)
    .filterExpression("tenant_id == '" + tenantId + "'")
    .build();
```

This translates to a `WHERE metadata->>'tenant_id' = ?` clause in the SQL query.

In Spring Boot, a `TenantContextFilter` (servlet filter) sets this at the start of every request:

```java
@Component
public class TenantContextFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) {
        String tenantId = extractTenantFromJwt(request);
        // Set PostgreSQL session variable for RLS
        entityManager.createNativeQuery(
            "SET LOCAL app.current_tenant = :tenant")
            .setParameter("tenant", tenantId)
            .executeUpdate();
        chain.doFilter(request, response);
    }
}
```

---

## 5. Encryption Strategy

### 5.1 Encrypted Columns

| Table               | Column(s)                                              |
|---------------------|--------------------------------------------------------|
| `users`             | `email_encrypted`                                      |
| `user_profiles`     | `phone_encrypted`, `address_encrypted`                 |
| `cv_details`        | `extracted_text`                                       |
| `motivation_letters`| `letter_text_encrypted`                                |
| `motivation_letter_history` | `letter_text_encrypted`                        |
| `job_source_configs`| `auth_config` (JSONB — encrypted values inside)        |

### 5.2 JPA AttributeConverter

```java
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final EncryptionService encryptionService;

    @Override
    public String convertToDatabaseColumn(String plainText) {
        return encryptionService.encrypt(plainText);  // AES-256-GCM
    }

    @Override
    public String convertToEntityAttribute(String cipherText) {
        return encryptionService.decrypt(cipherText);
    }
}
```

---

## 6. Auditing

All tables include `created_at` and `updated_at` (where applicable).  
Spring Data JPA `@EnableJpaAuditing` with `@CreatedDate` / `@LastModifiedDate` handles automatic timestamps.

For sensitive operations (data deletion, status changes), explicit event rows are written to `application_events` or published to Kafka for the audit log.

---

## 7. Migration Strategy

Database migrations are managed with **Flyway**:

```
src/main/resources/db/migration/
├── V1__create_users_profiles.sql
├── V2__create_oauth2_auth_server_tables.sql
├── V3__create_cv_details_embeddings.sql
├── V4__create_pgvector_store_rag.sql
├── V5__create_job_listings_embeddings.sql
├── V6__create_motivation_letters.sql
├── V7__create_applications_events.sql
├── V8__create_job_source_configs.sql
├── V9__enable_rls.sql
└── V10__create_indexes.sql
```

---

## 8. Performance Notes

| Concern                  | Strategy                                                       |
|--------------------------|----------------------------------------------------------------|
| Vector search latency    | HNSW index on `vector_store` (RAG); IVFFlat on `cv_embeddings`/`job_embeddings` (ranking). Consider HNSW everywhere for > 1M rows. |
| RAG retrieval quality    | Tune `topK` (5) and `similarityThreshold` (0.65–0.7) per use case; monitor retrieval precision. |
| Large job descriptions   | `TEXT` type (no size limit); compress at app level if > 100KB. |
| JSONB queries on CV data | GIN index on `parsed_json` for `@>` containment queries.       |
| JSONB metadata filtering | GIN index on `vector_store.metadata` for tenant-scoped RAG queries. |
| Connection pooling       | HikariCP (default); max pool = 20 per instance.               |
| Read replicas            | For analytics / stats queries, route to read replica.          |

---

## 9. Backup & Recovery

| Aspect       | Strategy                                                    |
|--------------|-------------------------------------------------------------|
| Backups      | Automated daily snapshots (RDS / Cloud SQL).                |
| PITR         | Point-in-time recovery with WAL archiving (7-day window).   |
| Cross-region | No cross-region replication (data residency); backups stay in-region. |
| Testing      | Monthly restore-from-backup drill.                          |
