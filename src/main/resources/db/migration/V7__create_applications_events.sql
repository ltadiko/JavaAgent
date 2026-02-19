-- ╔═══════════════════════════════════════════════════════════════════════════╗
-- ║  V7 — Applications, events, notes                                        ║
-- ╚═══════════════════════════════════════════════════════════════════════════╝

CREATE TABLE applications (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id          UUID NOT NULL,
    job_listing_id     UUID NOT NULL REFERENCES job_listings(id),
    cv_id              UUID NOT NULL REFERENCES cv_details(id),
    letter_id          UUID REFERENCES motivation_letters(id),
    status             VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    apply_method       VARCHAR(20),
    confirmation_ref   TEXT,
    failure_reason     TEXT,
    additional_message TEXT,
    submitted_at       TIMESTAMPTZ,
    version            INT NOT NULL DEFAULT 0,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, job_listing_id)
);

CREATE INDEX idx_applications_user ON applications(user_id, tenant_id);
CREATE INDEX idx_applications_status ON applications(status);
CREATE INDEX idx_applications_submitted ON applications(submitted_at DESC);

CREATE TABLE application_events (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    tenant_id      UUID NOT NULL,
    event_type     VARCHAR(30) NOT NULL,
    old_status     VARCHAR(30),
    new_status     VARCHAR(30),
    details        TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_application_events_app ON application_events(application_id);
CREATE INDEX idx_application_events_created ON application_events(created_at DESC);

CREATE TABLE application_notes (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    tenant_id      UUID NOT NULL,
    user_id        UUID NOT NULL REFERENCES users(id),
    note_text      TEXT NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_application_notes_app ON application_notes(application_id, tenant_id);
