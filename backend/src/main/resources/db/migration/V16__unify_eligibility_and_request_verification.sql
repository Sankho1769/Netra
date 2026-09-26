-- ============================================================================
-- V16: Unify Eligibility and Request Verification
-- ============================================================================

-- 1. Add biological_sex and verification fields to donor_profiles for recovery intervals and blood group verification
ALTER TABLE donor_profiles ADD COLUMN IF NOT EXISTS biological_sex VARCHAR(16);
ALTER TABLE donor_profiles ADD COLUMN IF NOT EXISTS verified_by UUID REFERENCES users(id);
ALTER TABLE donor_profiles ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE donor_profiles ADD COLUMN IF NOT EXISTS verification_notes VARCHAR(500);

-- 2. Add verification fields to blood_requests for anti-fraud trust model
ALTER TABLE blood_requests ADD COLUMN IF NOT EXISTS verification_status VARCHAR(32) NOT NULL DEFAULT 'UNVERIFIED';
ALTER TABLE blood_requests ADD COLUMN IF NOT EXISTS verified_by UUID REFERENCES users(id);
ALTER TABLE blood_requests ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE blood_requests ADD COLUMN IF NOT EXISTS verification_notes VARCHAR(500);

-- 3. Constraints
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_donor_profiles_biological_sex'
    ) THEN
        ALTER TABLE donor_profiles ADD CONSTRAINT chk_donor_profiles_biological_sex
            CHECK (biological_sex IS NULL OR biological_sex IN ('MALE', 'FEMALE', 'OTHER'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_blood_request_verification_status'
    ) THEN
        ALTER TABLE blood_requests ADD CONSTRAINT chk_blood_request_verification_status
            CHECK (verification_status IN ('UNVERIFIED', 'VERIFIED', 'REJECTED'));
    END IF;
END $$;

-- 4. Indexes
CREATE INDEX IF NOT EXISTS idx_donor_profiles_biological_sex ON donor_profiles(biological_sex);
CREATE INDEX IF NOT EXISTS idx_donor_profiles_verified_by ON donor_profiles(verified_by);
CREATE INDEX IF NOT EXISTS idx_blood_requests_verification_status ON blood_requests(verification_status);
