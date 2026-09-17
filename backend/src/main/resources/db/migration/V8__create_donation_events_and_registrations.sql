-- ============================================================================
-- V8: Create Donation Events and Event Registrations Schema
-- ============================================================================

CREATE TABLE IF NOT EXISTS donation_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    blood_bank_id UUID NOT NULL REFERENCES blood_banks(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    event_type VARCHAR(64) NOT NULL DEFAULT 'BLOOD_DONATION_CAMP',
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    venue_name VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    start_at TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at TIMESTAMP WITH TIME ZONE NOT NULL,
    registration_open_at TIMESTAMP WITH TIME ZONE NOT NULL,
    registration_close_at TIMESTAMP WITH TIME ZONE NOT NULL,
    donor_capacity INT NOT NULL,
    current_registration_count INT NOT NULL DEFAULT 0,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancelled_by UUID REFERENCES users(id),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_event_capacity CHECK (donor_capacity > 0),
    CONSTRAINT chk_event_registration_count CHECK (current_registration_count >= 0),
    CONSTRAINT chk_event_capacity_limit CHECK (current_registration_count <= donor_capacity),
    CONSTRAINT chk_event_type CHECK (event_type IN ('BLOOD_DONATION_CAMP')),
    CONSTRAINT chk_event_status CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'PUBLISHED', 'REGISTRATION_CLOSED', 'ONGOING', 'COMPLETED', 'CANCELLED', 'REJECTED')),
    CONSTRAINT chk_event_time_window CHECK (start_at < end_at),
    CONSTRAINT chk_event_reg_window CHECK (registration_open_at < registration_close_at),
    CONSTRAINT chk_event_reg_before_start CHECK (registration_close_at <= start_at),
    CONSTRAINT chk_event_latitude CHECK (latitude >= -90.0 AND latitude <= 90.0),
    CONSTRAINT chk_event_longitude CHECK (longitude >= -180.0 AND longitude <= 180.0)
);

CREATE INDEX IF NOT EXISTS idx_donation_events_status ON donation_events(status);
CREATE INDEX IF NOT EXISTS idx_donation_events_blood_bank_id ON donation_events(blood_bank_id);
CREATE INDEX IF NOT EXISTS idx_donation_events_start_at ON donation_events(start_at);
CREATE INDEX IF NOT EXISTS idx_donation_events_city ON donation_events(LOWER(city));
CREATE INDEX IF NOT EXISTS idx_donation_events_coords ON donation_events(latitude, longitude);

CREATE TABLE IF NOT EXISTS donation_event_registrations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID NOT NULL REFERENCES donation_events(id),
    donor_user_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(32) NOT NULL DEFAULT 'REGISTERED',
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    checked_in_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_event_donor UNIQUE (event_id, donor_user_id),
    CONSTRAINT chk_registration_status CHECK (status IN ('REGISTERED', 'WAITLISTED', 'CANCELLED', 'CHECKED_IN', 'COMPLETED', 'NO_SHOW', 'REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_event_reg_event_id ON donation_event_registrations(event_id);
CREATE INDEX IF NOT EXISTS idx_event_reg_donor_user_id ON donation_event_registrations(donor_user_id);
CREATE INDEX IF NOT EXISTS idx_event_reg_status ON donation_event_registrations(status);
