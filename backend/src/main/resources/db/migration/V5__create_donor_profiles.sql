-- ==========================================================
-- NETRA Platform: Donor Profiles Schema
-- Version: V5 (PostgreSQL / Supabase compatible)
-- Note: Strict clinical and identity separation.
-- User identity holds basic contact/auth data.
-- DonorProfile holds donation preferences, controlled blood group,
-- verification status, and availability state.
-- ==========================================================

CREATE TABLE IF NOT EXISTS donor_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    blood_group VARCHAR(8) NOT NULL,
    blood_group_verification_status VARCHAR(32) NOT NULL DEFAULT 'SELF_REPORTED',
    availability_status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
    donor_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    last_donation_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_donor_profiles_blood_group CHECK (blood_group IN ('A+', 'A-', 'B+', 'B-', 'O+', 'O-', 'AB+', 'AB-')),
    CONSTRAINT chk_donor_profiles_bg_verification CHECK (blood_group_verification_status IN ('SELF_REPORTED', 'VERIFIED')),
    CONSTRAINT chk_donor_profiles_availability CHECK (availability_status IN ('AVAILABLE', 'UNAVAILABLE', 'PAUSED')),
    CONSTRAINT chk_donor_profiles_donor_status CHECK (donor_status IN ('ACTIVE', 'PAUSED', 'INACTIVE'))
);

CREATE INDEX IF NOT EXISTS idx_donor_profiles_user_id ON donor_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_donor_profiles_blood_group ON donor_profiles(blood_group);
CREATE INDEX IF NOT EXISTS idx_donor_profiles_availability ON donor_profiles(availability_status);
CREATE INDEX IF NOT EXISTS idx_donor_profiles_donor_status ON donor_profiles(donor_status);
