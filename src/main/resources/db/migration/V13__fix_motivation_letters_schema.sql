-- ╔═══════════════════════════════════════════════════════════════════════════╗
-- ║  V13 — Fix motivation_letters schema alignment with JPA entity            ║
-- ╚═══════════════════════════════════════════════════════════════════════════╝

-- Add 'edited_content' column
ALTER TABLE motivation_letters ADD COLUMN IF NOT EXISTS edited_content TEXT;

-- Make cv_id and job_listing_id nullable (entity allows null)
ALTER TABLE motivation_letters ALTER COLUMN cv_id DROP NOT NULL;
ALTER TABLE motivation_letters ALTER COLUMN job_listing_id DROP NOT NULL;
ALTER TABLE motivation_letters ALTER COLUMN letter_text_encrypted DROP NOT NULL;
