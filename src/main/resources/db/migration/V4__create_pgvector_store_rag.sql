-- ╔═══════════════════════════════════════════════════════════════════════════╗
-- ║  V4 — PgVectorStore table for RAG (Spring AI managed)                    ║
-- ╚═══════════════════════════════════════════════════════════════════════════╝

CREATE TABLE IF NOT EXISTS vector_store (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content   TEXT NOT NULL,
    metadata  JSONB NOT NULL DEFAULT '{}',
    embedding vector(${embedding_dimensions})
);

CREATE INDEX idx_vector_store_embedding ON vector_store
    USING hnsw (embedding vector_cosine_ops);

-- Custom index for tenant-scoped RAG queries
CREATE INDEX idx_vector_store_metadata ON vector_store
    USING gin (metadata jsonb_path_ops);
