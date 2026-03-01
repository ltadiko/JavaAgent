-- ╔═══════════════════════════════════════════════════════════════════════════╗
-- ║  V9 — Add missing email column to users table                            ║
-- ╚═══════════════════════════════════════════════════════════════════════════╝
-- This migration adds the email column if it doesn't exist.
-- The column was defined in V1 but may be missing in some existing databases.
-- Also makes email_encrypted nullable for MVP (will be re-added in Sprint 10).

DO $$
BEGIN
    -- Add email column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'email'
    ) THEN
        ALTER TABLE users ADD COLUMN email TEXT;
        -- Update existing rows with a placeholder (should be updated with real data)
        UPDATE users SET email = 'migrated-' || id::text || '@placeholder.local' WHERE email IS NULL;
        -- Now add the NOT NULL constraint
        ALTER TABLE users ALTER COLUMN email SET NOT NULL;
    END IF;

    -- Make email_encrypted nullable for MVP if it exists (encryption deferred to Sprint 10)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'email_encrypted'
    ) THEN
        ALTER TABLE users ALTER COLUMN email_encrypted DROP NOT NULL;
    END IF;
END $$;
