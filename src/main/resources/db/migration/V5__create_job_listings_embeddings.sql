-- ╔═══════════════════════════════════════════════════════════════════════════╗
-- ║  V5 — Job listings, embeddings, saved jobs, source configs               ║
-- ╚═══════════════════════════════════════════════════════════════════════════╝

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
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_listing_id UUID NOT NULL REFERENCES job_listings(id) ON DELETE CASCADE,
    tenant_id      UUID NOT NULL,
    embedding      vector(${embedding_dimensions}),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_job_embeddings_job ON job_embeddings(job_listing_id);
CREATE INDEX idx_job_embeddings_vector ON job_embeddings
    USING hnsw (embedding vector_cosine_ops);

CREATE TABLE saved_jobs (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_listing_id UUID NOT NULL REFERENCES job_listings(id) ON DELETE CASCADE,
    tenant_id      UUID NOT NULL,
    saved_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, job_listing_id)
);

CREATE TABLE job_source_configs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL,
    name         VARCHAR(100) NOT NULL,
    base_url     TEXT NOT NULL,
    scraper_type VARCHAR(30) NOT NULL,
    auth_config  JSONB,
    enabled      BOOLEAN NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_job_source_configs_tenant ON job_source_configs(tenant_id, enabled);
