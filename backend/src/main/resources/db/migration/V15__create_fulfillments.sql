-- ==========================================================
-- NETRA Platform: Fulfillment Aggregate Schema
-- Version: V15 (PostgreSQL / Supabase compatible)
-- Note on UUID default: Uses built-in gen_random_uuid() available natively in PostgreSQL 13+ and Supabase.
-- ==========================================================

-- 1. Add units_fulfilled to blood_requests for atomic quantity accounting
ALTER TABLE blood_requests ADD COLUMN IF NOT EXISTS units_fulfilled INT NOT NULL DEFAULT 0;
ALTER TABLE blood_requests ADD CONSTRAINT chk_request_units_fulfilled
    CHECK (units_fulfilled >= 0 AND units_fulfilled <= units_required);

-- 2. Create fulfillments table
CREATE TABLE IF NOT EXISTS fulfillments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blood_request_id UUID NOT NULL,
    donation_id UUID NOT NULL,
    units INT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'READY',
    created_by_user_id UUID NOT NULL,
    started_by_user_id UUID,
    completed_by_user_id UUID,
    failed_by_user_id UUID,
    cancelled_by_user_id UUID,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    failed_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    failure_reason VARCHAR(500),
    cancellation_reason VARCHAR(500),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_fulfillments_blood_request
        FOREIGN KEY (blood_request_id)
        REFERENCES blood_requests(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_fulfillments_donation
        FOREIGN KEY (donation_id)
        REFERENCES donations(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_fulfillments_created_by
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_fulfillments_started_by
        FOREIGN KEY (started_by_user_id)
        REFERENCES users(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_fulfillments_completed_by
        FOREIGN KEY (completed_by_user_id)
        REFERENCES users(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_fulfillments_failed_by
        FOREIGN KEY (failed_by_user_id)
        REFERENCES users(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_fulfillments_cancelled_by
        FOREIGN KEY (cancelled_by_user_id)
        REFERENCES users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_fulfillments_status
        CHECK (status IN ('READY', 'IN_PROGRESS', 'FULFILLED', 'CANCELLED', 'FAILED')),

    CONSTRAINT chk_fulfillments_units
        CHECK (units > 0 AND units <= 50)
);

CREATE INDEX IF NOT EXISTS idx_fulfillments_blood_request ON fulfillments(blood_request_id);
CREATE INDEX IF NOT EXISTS idx_fulfillments_donation ON fulfillments(donation_id);
CREATE INDEX IF NOT EXISTS idx_fulfillments_status ON fulfillments(status);
CREATE INDEX IF NOT EXISTS idx_fulfillments_created_by ON fulfillments(created_by_user_id);
CREATE INDEX IF NOT EXISTS idx_fulfillments_created_at ON fulfillments(created_at DESC);

-- Unique index to prevent the same verified donation from being consumed or reserved twice
CREATE UNIQUE INDEX IF NOT EXISTS uq_fulfillments_active_donation 
    ON fulfillments(donation_id) 
    WHERE status NOT IN ('CANCELLED', 'FAILED');

-- 3. Update notifications check constraint to allow fulfillment notification types
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
        'DONATION_REJECTED',
        'FULFILLMENT_CREATED',
        'FULFILLMENT_STARTED',
        'FULFILLMENT_COMPLETED',
        'FULFILLMENT_FAILED',
        'FULFILLMENT_CANCELLED'
    ));
