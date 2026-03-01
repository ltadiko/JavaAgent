-- ╔═══════════════════════════════════════════════════════════════════════════╗
-- ║  V10 — Make email_encrypted nullable for MVP                             ║
-- ╚═══════════════════════════════════════════════════════════════════════════╝
-- Email encryption is deferred to Sprint 10 (Production Hardening).
-- For MVP, we use plaintext email column. Make email_encrypted nullable.

DO $$
BEGIN
    -- Make email_encrypted nullable if the column exists
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'email_encrypted'
    ) THEN
        ALTER TABLE users ALTER COLUMN email_encrypted DROP NOT NULL;
    END IF;
END $$;
