-- ╔═══════════════════════════════════════════════════════════════════════════╗
-- ║  V1 — Core tables: users, user_profiles                                 ║
-- ╚═══════════════════════════════════════════════════════════════════════════╝

-- Required extensions
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "vector";

-- ─── Users ───────────────────────────────────────────────────────────────
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

-- ─── User Profiles ───────────────────────────────────────────────────────
CREATE TABLE user_profiles (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id            UUID NOT NULL,
    phone_encrypted      TEXT,
    address_encrypted    TEXT,
    linkedin_url         TEXT,
    preferred_job_titles TEXT[],
    preferred_locations  TEXT[],
    preferred_remote     BOOLEAN DEFAULT false,
    preferred_salary_min BIGINT,
    preferred_currency   VARCHAR(3),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_user_profiles_user ON user_profiles(user_id);
CREATE INDEX idx_user_profiles_tenant ON user_profiles(tenant_id);
