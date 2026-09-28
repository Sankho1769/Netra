-- ============================================================================
-- NETRA Platform: Karma System & Hospital Verification Registry
-- Version: V18 (PostgreSQL / Supabase compatible)
-- ============================================================================

-- 1. Karma Accounts Table
CREATE TABLE IF NOT EXISTS karma_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    balance INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_karma_accounts_user_id ON karma_accounts(user_id);

-- 2. Karma Transactions Ledger Table
CREATE TABLE IF NOT EXISTS karma_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES karma_accounts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    points INT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    reference_type VARCHAR(64),
    reference_id VARCHAR(128),
    reason VARCHAR(500) NOT NULL,
    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    previous_balance INT NOT NULL,
    resulting_balance INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_karma_tx_user_created ON karma_transactions(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_karma_tx_account ON karma_transactions(account_id);

-- Unique index to guarantee idempotency and prevent duplicate point awards
CREATE UNIQUE INDEX IF NOT EXISTS uq_karma_tx_reference_event 
    ON karma_transactions(reference_type, reference_id, event_type)
    WHERE reference_type IS NOT NULL AND reference_id IS NOT NULL;

-- 3. Verified Hospitals & Places Registry Table
CREATE TABLE IF NOT EXISTS verified_hospitals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    place_id VARCHAR(128) UNIQUE,
    has_blood_bank BOOLEAN NOT NULL DEFAULT FALSE,
    verification_status VARCHAR(32) NOT NULL DEFAULT 'VERIFIED',
    phone VARCHAR(32),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_hospital_verification_status 
        CHECK (verification_status IN ('VERIFIED', 'UNVERIFIED', 'REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_verified_hospitals_city ON verified_hospitals(city);
CREATE INDEX IF NOT EXISTS idx_verified_hospitals_name ON verified_hospitals(name);

-- 4. Add Hospital Verification Fields to Blood Requests
ALTER TABLE blood_requests ADD COLUMN IF NOT EXISTS verified_hospital_id UUID REFERENCES verified_hospitals(id) ON DELETE SET NULL;
ALTER TABLE blood_requests ADD COLUMN IF NOT EXISTS hospital_verification_status VARCHAR(32) NOT NULL DEFAULT 'UNVERIFIED';

-- 5. Seed Authoritative Initial Verified Hospitals
INSERT INTO verified_hospitals (id, name, address, city, state, postal_code, latitude, longitude, place_id, has_blood_bank, verification_status, phone)
VALUES 
    (gen_random_uuid(), 'KEM Hospital, Parel', 'Acharya Donde Marg, Parel', 'Mumbai', 'Maharashtra', '400012', 18.9986, 72.8426, 'PLACE-IN-MUM-KEM', true, 'VERIFIED', '+912224107000'),
    (gen_random_uuid(), 'AIIMS New Delhi', 'Sri Aurobindo Marg, Ansari Nagar', 'New Delhi', 'Delhi', '110029', 28.5672, 77.2100, 'PLACE-IN-DEL-AIIMS', true, 'VERIFIED', '+911126588500'),
    (gen_random_uuid(), 'AMRI Hospital, Dhakuria', 'Block A, Scheme LII, P-4&5, Gariahat Rd, Dhakuria', 'Kolkata', 'West Bengal', '700029', 22.5113, 88.3683, 'PLACE-IN-CCU-AMRI', true, 'VERIFIED', '+913366800000'),
    (gen_random_uuid(), 'Fortis Hospital, Anandapur', '730, Anandapur, E.M. Bypass Road', 'Kolkata', 'West Bengal', '700107', 22.5186, 88.4014, 'PLACE-IN-CCU-FORTIS', true, 'VERIFIED', '+913366284444'),
    (gen_random_uuid(), 'Apollo Hospital, Greams Road', '21 Greams Lane, Off Greams Road', 'Chennai', 'Tamil Nadu', '600006', 13.0604, 80.2505, 'PLACE-IN-MAA-APOLLO', true, 'VERIFIED', '+914428290200'),
    (gen_random_uuid(), 'PGIMER Chandigarh', 'Sector 12', 'Chandigarh', 'Chandigarh', '160012', 30.7645, 76.7744, 'PLACE-IN-IXC-PGIMER', true, 'VERIFIED', '+911722747585'),
    (gen_random_uuid(), 'Manipal Hospital, HAL Airport Road', '98, HAL Old Airport Rd, Kodihalli', 'Bengaluru', 'Karnataka', '560017', 12.9592, 77.6534, 'PLACE-IN-BLR-MANIPAL', true, 'VERIFIED', '+918025024444')
ON CONFLICT (place_id) DO NOTHING;
