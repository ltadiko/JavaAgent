-- ╔═══════════════════════════════════════════════════════════════════════════╗
-- ║  V14 — Add missing columns to applications table                          ║
-- ╚═══════════════════════════════════════════════════════════════════════════╝

-- Add timestamp columns for application lifecycle tracking
ALTER TABLE applications ADD COLUMN IF NOT EXISTS sent_at TIMESTAMPTZ;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS viewed_at TIMESTAMPTZ;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS response_at TIMESTAMPTZ;
