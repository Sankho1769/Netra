-- ==========================================================
-- NETRA Platform: Add Capability Token Hash for Anonymous Sessions
-- Version: V3
-- ==========================================================

ALTER TABLE eligibility_sessions ADD COLUMN IF NOT EXISTS capability_token_hash VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_sessions_capability_token_hash ON eligibility_sessions(capability_token_hash);
