-- ╔═══════════════════════════════════════════════════════════════════════════╗
-- ║  V12 — Add missing columns to cv_details                                 ║
-- ╚═══════════════════════════════════════════════════════════════════════════╝

-- Add 'active' column (entity field: Boolean active, default true)
ALTER TABLE cv_details ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT true;

-- Add 'parsed_at' column
ALTER TABLE cv_details ADD COLUMN IF NOT EXISTS parsed_at TIMESTAMPTZ;

-- Add 'error_message' column
ALTER TABLE cv_details ADD COLUMN IF NOT EXISTS error_message TEXT;

-- Rename file_size_bytes to file_size if it exists (entity maps to file_size)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'cv_details' AND column_name = 'file_size_bytes')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_name = 'cv_details' AND column_name = 'file_size')
    THEN
        ALTER TABLE cv_details RENAME COLUMN file_size_bytes TO file_size;
    END IF;
END $$;


