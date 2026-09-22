-- ==========================================================
-- NETRA Platform: Verified Donation Model Schema
-- Version: V14 (PostgreSQL / Supabase compatible)
-- Note on UUID default: Uses built-in gen_random_uuid() available natively in PostgreSQL 13+ and Supabase.
-- ==========================================================

-- 1. Create Donations Table
CREATE TABLE IF NOT EXISTS donations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    donor_user_id UUID NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    blood_request_id UUID,
    donation_event_id UUID,
    donation_date DATE NOT NULL,
    verification_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    verified_at TIMESTAMP WITH TIME ZONE,
    verified_by_user_id UUID,
    rejection_reason VARCHAR(500),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_donations_donor
        FOREIGN KEY (donor_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_donations_blood_request
        FOREIGN KEY (blood_request_id)
        REFERENCES blood_requests(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_donations_donation_event
        FOREIGN KEY (donation_event_id)
        REFERENCES donation_events(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_donations_verified_by
        FOREIGN KEY (verified_by_user_id)
        REFERENCES users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_donations_source_type
        CHECK (source_type IN ('BLOOD_REQUEST', 'DONATION_EVENT')),

    CONSTRAINT chk_donations_verification_status
        CHECK (verification_status IN ('PENDING_VERIFICATION', 'VERIFIED', 'REJECTED', 'CANCELLED')),

    CONSTRAINT chk_donations_source_references
        CHECK (
            (source_type = 'BLOOD_REQUEST' AND blood_request_id IS NOT NULL AND donation_event_id IS NULL)
            OR
            (source_type = 'DONATION_EVENT' AND donation_event_id IS NOT NULL AND blood_request_id IS NULL)
        ),

    CONSTRAINT uq_donations_donor_blood_request
        UNIQUE (donor_user_id, blood_request_id),

    CONSTRAINT uq_donations_donor_event
        UNIQUE (donor_user_id, donation_event_id)
);

CREATE INDEX IF NOT EXISTS idx_donations_donor_status_date ON donations(donor_user_id, verification_status, donation_date DESC);
CREATE INDEX IF NOT EXISTS idx_donations_status_created ON donations(verification_status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_donations_blood_request ON donations(blood_request_id);
CREATE INDEX IF NOT EXISTS idx_donations_donation_event ON donations(donation_event_id);

-- 2. Update notifications check constraint to allow donation event notification types
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS chk_notifications_type;
ALTER TABLE notifications ADD CONSTRAINT chk_notifications_type
    CHECK (type IN (
        'MATCH_CREATED',
        'MATCH_ACCEPTED',
        'MATCH_DECLINED',
        'MATCH_EXPIRED',
        'BLOOD_REQUEST_CANCELLED',
        'EMERGENCY_REQUEST_CREATED',
        'DONATION_SUBMITTED',
        'DONATION_VERIFIED',
        'DONATION_REJECTED'
    ));
