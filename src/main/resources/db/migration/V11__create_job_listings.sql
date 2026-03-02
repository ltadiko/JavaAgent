-- Sprint 5.3: Add missing columns to job_listings table
-- V5 created the base table; this migration adds columns needed by the JPA entity

ALTER TABLE job_listings ADD COLUMN IF NOT EXISTS requirements TEXT;
ALTER TABLE job_listings ADD COLUMN IF NOT EXISTS skills JSONB DEFAULT '[]'::jsonb;
ALTER TABLE job_listings ADD COLUMN IF NOT EXISTS salary_min DECIMAL(12, 2);
ALTER TABLE job_listings ADD COLUMN IF NOT EXISTS salary_max DECIMAL(12, 2);
ALTER TABLE job_listings ADD COLUMN IF NOT EXISTS salary_currency VARCHAR(3) DEFAULT 'EUR';
ALTER TABLE job_listings ADD COLUMN IF NOT EXISTS employment_type VARCHAR(50);
ALTER TABLE job_listings ADD COLUMN IF NOT EXISTS remote_type VARCHAR(50);
ALTER TABLE job_listings ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE job_listings ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();

-- Indexes for the new columns
CREATE INDEX IF NOT EXISTS idx_job_status ON job_listings(status);
CREATE INDEX IF NOT EXISTS idx_job_location ON job_listings(location);
CREATE INDEX IF NOT EXISTS idx_job_company ON job_listings(company);

-- GIN index for JSONB skills
CREATE INDEX IF NOT EXISTS idx_job_skills ON job_listings USING GIN(skills);

-- Composite index for tenant + status
CREATE INDEX IF NOT EXISTS idx_job_tenant_status ON job_listings(tenant_id, status);

-- Full-text search index
CREATE INDEX IF NOT EXISTS idx_job_fulltext ON job_listings
    USING GIN(to_tsvector('english', coalesce(title, '') || ' ' || coalesce(description, '')));

COMMENT ON TABLE job_listings IS 'Multi-tenant job listings with skills matching support';
COMMENT ON COLUMN job_listings.skills IS 'JSONB array of skill strings for matching';
