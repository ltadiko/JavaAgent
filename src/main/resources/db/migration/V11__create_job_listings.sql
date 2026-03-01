-- Sprint 5.3: Create job_listings table
-- Multi-tenant job listings with JSONB skills for efficient search

CREATE TABLE IF NOT EXISTS job_listings (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    title             VARCHAR(500) NOT NULL,
    company           VARCHAR(255) NOT NULL,
    location          VARCHAR(255),
    description       TEXT,
    requirements      TEXT,
    skills            JSONB DEFAULT '[]'::jsonb,
    salary_min        DECIMAL(12, 2),
    salary_max        DECIMAL(12, 2),
    salary_currency   VARCHAR(3) DEFAULT 'EUR',
    employment_type   VARCHAR(50),
    remote_type       VARCHAR(50),
    source_url        VARCHAR(2000),
    external_id       VARCHAR(255),
    status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at        TIMESTAMP WITH TIME ZONE
);

-- Indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_job_tenant_id ON job_listings(tenant_id);
CREATE INDEX IF NOT EXISTS idx_job_status ON job_listings(status);
CREATE INDEX IF NOT EXISTS idx_job_location ON job_listings(location);
CREATE INDEX IF NOT EXISTS idx_job_company ON job_listings(company);
CREATE INDEX IF NOT EXISTS idx_job_created_at ON job_listings(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_job_expires_at ON job_listings(expires_at) WHERE expires_at IS NOT NULL;

-- GIN index for JSONB skills - enables fast skill-based searches
CREATE INDEX IF NOT EXISTS idx_job_skills ON job_listings USING GIN(skills);

-- Composite index for tenant + status (most common query pattern)
CREATE INDEX IF NOT EXISTS idx_job_tenant_status ON job_listings(tenant_id, status);

-- Full-text search index for title and description
CREATE INDEX IF NOT EXISTS idx_job_fulltext ON job_listings
    USING GIN(to_tsvector('english', coalesce(title, '') || ' ' || coalesce(description, '')));

-- Unique constraint for external_id per tenant (prevent duplicate imports)
CREATE UNIQUE INDEX IF NOT EXISTS idx_job_tenant_external
    ON job_listings(tenant_id, external_id) WHERE external_id IS NOT NULL;

-- Add comment for documentation
COMMENT ON TABLE job_listings IS 'Multi-tenant job listings with skills matching support';
COMMENT ON COLUMN job_listings.skills IS 'JSONB array of skill strings for matching';
COMMENT ON COLUMN job_listings.external_id IS 'ID from external source (LinkedIn, Indeed, etc.)';
