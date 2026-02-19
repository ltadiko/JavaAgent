-- ╔═══════════════════════════════════════════════════════════════════════════╗
-- ║  V6 — Motivation letters & version history                               ║
-- ╚═══════════════════════════════════════════════════════════════════════════╝

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
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    letter_id             UUID NOT NULL REFERENCES motivation_letters(id) ON DELETE CASCADE,
    tenant_id             UUID NOT NULL,
    version               INT NOT NULL,
    letter_text_encrypted TEXT NOT NULL,
    generated_at          TIMESTAMPTZ NOT NULL,
    UNIQUE (letter_id, version)
);
