-- ============================================================================
-- V9: Create Blood Requests Schema
-- ============================================================================

CREATE TABLE IF NOT EXISTS blood_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    requester_user_id UUID NOT NULL REFERENCES users(id),
    blood_group VARCHAR(16) NOT NULL,
    units_required INT NOT NULL,
    urgency VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    hospital_name VARCHAR(255) NOT NULL,
    hospital_address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    required_by TIMESTAMP WITH TIME ZONE NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancelled_by UUID REFERENCES users(id),
    cancellation_reason VARCHAR(255),
    fulfilled_at TIMESTAMP WITH TIME ZONE,
    fulfilled_by UUID REFERENCES users(id),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_request_units CHECK (units_required > 0 AND units_required <= 50),
    CONSTRAINT chk_request_blood_group CHECK (blood_group IN ('A+', 'A-', 'B+', 'B-', 'O+', 'O-', 'AB+', 'AB-')),
    CONSTRAINT chk_request_urgency CHECK (urgency IN ('NORMAL', 'URGENT', 'CRITICAL')),
    CONSTRAINT chk_request_status CHECK (status IN ('OPEN', 'FULFILLED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT chk_request_latitude CHECK (latitude >= -90.0 AND latitude <= 90.0),
    CONSTRAINT chk_request_longitude CHECK (longitude >= -180.0 AND longitude <= 180.0)
);

CREATE INDEX IF NOT EXISTS idx_blood_requests_status ON blood_requests(status);
CREATE INDEX IF NOT EXISTS idx_blood_requests_requester ON blood_requests(requester_user_id);
CREATE INDEX IF NOT EXISTS idx_blood_requests_blood_group ON blood_requests(blood_group);
CREATE INDEX IF NOT EXISTS idx_blood_requests_urgency ON blood_requests(urgency);
CREATE INDEX IF NOT EXISTS idx_blood_requests_city ON blood_requests(LOWER(city));
CREATE INDEX IF NOT EXISTS idx_blood_requests_coords ON blood_requests(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_blood_requests_required_by ON blood_requests(required_by);
