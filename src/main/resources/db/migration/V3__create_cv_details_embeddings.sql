-- ╔═══════════════════════════════════════════════════════════════════════════╗
-- ║  V3 — CV details & embeddings                                            ║
-- ╚═══════════════════════════════════════════════════════════════════════════╝

CREATE TABLE cv_details (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id        UUID NOT NULL,
    file_name        TEXT NOT NULL,
    s3_key           TEXT NOT NULL,
    content_type     VARCHAR(100) NOT NULL,
    file_size_bytes  BIGINT NOT NULL,
    extracted_text   TEXT,
    parsed_json      JSONB NOT NULL DEFAULT '{}',
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_cv_details_user ON cv_details(user_id, tenant_id);
CREATE INDEX idx_cv_details_status ON cv_details(user_id, status);

CREATE TABLE cv_embeddings (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cv_id      UUID NOT NULL REFERENCES cv_details(id) ON DELETE CASCADE,
    tenant_id  UUID NOT NULL,
    embedding  vector(${embedding_dimensions}),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_cv_embeddings_cv ON cv_embeddings(cv_id);
CREATE INDEX idx_cv_embeddings_vector ON cv_embeddings
    USING hnsw (embedding vector_cosine_ops);
