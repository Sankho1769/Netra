-- ============================================================================
-- V21: Allow UNVERIFIED User Status
-- ============================================================================

-- Drop legacy check constraints on users status column if present
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_status;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_status_check;
