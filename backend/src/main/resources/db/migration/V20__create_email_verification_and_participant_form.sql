-- ============================================================================
-- V20: Email Verification Challenges and Donation Camp Participant Form Schema
-- ============================================================================

-- 1. Add email verification timestamp to users
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMP WITH TIME ZONE;
UPDATE users SET email_verified_at = created_at WHERE email_verified_at IS NULL AND status = 'ACTIVE';

-- 2. Create email verification challenges table
CREATE TABLE IF NOT EXISTS email_verification_challenges (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_email_verification_email ON email_verification_challenges(LOWER(email));
CREATE INDEX IF NOT EXISTS idx_email_verification_user_id ON email_verification_challenges(user_id);
CREATE INDEX IF NOT EXISTS idx_email_verification_created_at ON email_verification_challenges(created_at);

-- 3. Add participant form fields to donation_event_registrations
ALTER TABLE donation_event_registrations ADD COLUMN IF NOT EXISTS participant_name VARCHAR(128);
ALTER TABLE donation_event_registrations ADD COLUMN IF NOT EXISTS participant_dob DATE;
ALTER TABLE donation_event_registrations ADD COLUMN IF NOT EXISTS participant_phone VARCHAR(32);
ALTER TABLE donation_event_registrations ADD COLUMN IF NOT EXISTS participant_email VARCHAR(255);
ALTER TABLE donation_event_registrations ADD COLUMN IF NOT EXISTS participant_blood_group VARCHAR(16);
ALTER TABLE donation_event_registrations ADD COLUMN IF NOT EXISTS participant_address VARCHAR(255);
ALTER TABLE donation_event_registrations ADD COLUMN IF NOT EXISTS participant_city VARCHAR(100);
ALTER TABLE donation_event_registrations ADD COLUMN IF NOT EXISTS emergency_contact_name VARCHAR(128);
ALTER TABLE donation_event_registrations ADD COLUMN IF NOT EXISTS emergency_contact_phone VARCHAR(32);
ALTER TABLE donation_event_registrations ADD COLUMN IF NOT EXISTS consent_confirmed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE donation_event_registrations ADD COLUMN IF NOT EXISTS consent_timestamp TIMESTAMP WITH TIME ZONE;
