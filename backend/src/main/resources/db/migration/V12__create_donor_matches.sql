-- ==========================================================
-- NETRA Platform: Persistent Donor Matching & Response Model
-- Version: V12 (PostgreSQL / Supabase compatible)
-- Note on UUID default: Uses built-in gen_random_uuid() available natively in PostgreSQL 13+ and Supabase.
-- Earlier migrations used uuid_generate_v4() via uuid-ossp; gen_random_uuid() is standard for modern PostgreSQL.
-- ==========================================================

CREATE TABLE IF NOT EXISTS donor_matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blood_request_id UUID NOT NULL,
    donor_user_id UUID NOT NULL,
    response_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_donor_matches_request
        FOREIGN KEY (blood_request_id)
        REFERENCES blood_requests(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_donor_matches_donor
        FOREIGN KEY (donor_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_donor_matches_status
        CHECK (response_status IN ('MATCHED', 'ACCEPTED', 'DECLINED', 'EXPIRED', 'CANCELLED')),

    CONSTRAINT uq_donor_matches_request_donor
        UNIQUE (blood_request_id, donor_user_id)
);

CREATE INDEX IF NOT EXISTS idx_donor_matches_blood_request_id ON donor_matches(blood_request_id);
CREATE INDEX IF NOT EXISTS idx_donor_matches_donor_user_id ON donor_matches(donor_user_id);
CREATE INDEX IF NOT EXISTS idx_donor_matches_status_expires ON donor_matches(response_status, expires_at);
