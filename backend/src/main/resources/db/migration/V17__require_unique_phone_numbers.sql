-- ============================================================================
-- V17: Require Unique Phone Numbers on Users Table
-- ============================================================================

-- 1. Backfill any existing users with null or empty phone numbers
UPDATE users
SET phone = '+919' || LPAD(SUBSTRING(REPLACE(id::text, '-', ''), 1, 9), 9, '0')
WHERE phone IS NULL OR TRIM(phone) = '';

-- 2. Alter column to NOT NULL
ALTER TABLE users ALTER COLUMN phone SET NOT NULL;

-- 3. Add UNIQUE constraint safely if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_users_phone'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT uq_users_phone UNIQUE (phone);
    END IF;
END $$;

-- 4. Create index for fast phone lookups
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);
