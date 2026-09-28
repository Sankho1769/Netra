-- ============================================================================
-- NETRA Platform: Expand Hospital Registry Source Tracking & Lifecycle Statuses
-- Version: V19 (PostgreSQL / Supabase compatible)
-- ============================================================================

-- 1. Update blood_requests status check constraint
ALTER TABLE blood_requests DROP CONSTRAINT IF EXISTS chk_request_status;
ALTER TABLE blood_requests ADD CONSTRAINT chk_request_status 
    CHECK (status IN ('OPEN', 'VERIFIED', 'FULFILLED', 'CANCELLED', 'EXPIRED', 'FLAGGED', 'CONFIRMED_FAKE', 'REJECTED'));

-- 2. Update donor_matches response_status check constraint
ALTER TABLE donor_matches DROP CONSTRAINT IF EXISTS chk_donor_matches_status;
ALTER TABLE donor_matches ADD CONSTRAINT chk_donor_matches_status 
    CHECK (response_status IN ('MATCHED', 'ACCEPTED', 'DECLINED', 'EXPIRED', 'CANCELLED', 'ARRIVED', 'CONFIRMED_NO_SHOW', 'MEDICAL_REJECTION', 'CANCELLED_SAFE'));

-- 3. Enhance verified_hospitals with source tracking and place_type
ALTER TABLE verified_hospitals ADD COLUMN IF NOT EXISTS source VARCHAR(32) NOT NULL DEFAULT 'INTERNAL_REGISTRY';
ALTER TABLE verified_hospitals ADD COLUMN IF NOT EXISTS place_type VARCHAR(64) DEFAULT 'HOSPITAL';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_verified_hospitals_source'
    ) THEN
        ALTER TABLE verified_hospitals ADD CONSTRAINT chk_verified_hospitals_source
            CHECK (source IN ('INTERNAL_REGISTRY', 'EXTERNAL_PROVIDER', 'USER_SUBMISSION'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_verified_hospitals_source ON verified_hospitals(source);

-- 4. Seed authoritative national hospitals across major medical hubs
INSERT INTO verified_hospitals (id, name, address, city, state, postal_code, latitude, longitude, place_id, has_blood_bank, verification_status, phone, source, place_type)
VALUES
    (gen_random_uuid(), 'SSKM Hospital (IPGMER)', '244 AJC Bose Road, Bhowanipore', 'Kolkata', 'West Bengal', '700020', 22.5398, 88.3426, 'PLACE-IN-CCU-SSKM', true, 'VERIFIED', '+913322231589', 'INTERNAL_REGISTRY', 'HOSPITAL'),
    (gen_random_uuid(), 'Medical College and Hospital, Kolkata', '88 College Street, College Square', 'Kolkata', 'West Bengal', '700073', 22.5735, 88.3629, 'PLACE-IN-CCU-MCH', true, 'VERIFIED', '+913322551621', 'INTERNAL_REGISTRY', 'HOSPITAL'),
    (gen_random_uuid(), 'NRS Medical College and Hospital', '138 AJC Bose Road, Sealdah', 'Kolkata', 'West Bengal', '700014', 22.5645, 88.3698, 'PLACE-IN-CCU-NRS', true, 'VERIFIED', '+913322860033', 'INTERNAL_REGISTRY', 'HOSPITAL'),
    (gen_random_uuid(), 'Safdarjung Hospital', 'Ring Road, Opposite AIIMS', 'New Delhi', 'Delhi', '110029', 28.5702, 77.2081, 'PLACE-IN-DEL-SAFDARJUNG', true, 'VERIFIED', '+911126165060', 'INTERNAL_REGISTRY', 'HOSPITAL'),
    (gen_random_uuid(), 'Max Super Speciality Hospital, Saket', '1 2, Press Enclave Marg, Saket', 'New Delhi', 'Delhi', '110017', 28.5283, 77.2117, 'PLACE-IN-DEL-MAXSAKET', true, 'VERIFIED', '+911126515050', 'INTERNAL_REGISTRY', 'HOSPITAL'),
    (gen_random_uuid(), 'Narayana Institute of Cardiac Sciences', '258/A, Bommasandra Industrial Area', 'Bengaluru', 'Karnataka', '560099', 12.8152, 77.6942, 'PLACE-IN-BLR-NARAYANA', true, 'VERIFIED', '+918071222222', 'INTERNAL_REGISTRY', 'HOSPITAL'),
    (gen_random_uuid(), 'Tata Memorial Hospital', 'Dr. E Borges Road, Parel', 'Mumbai', 'Maharashtra', '400012', 19.0048, 72.8433, 'PLACE-IN-BOM-TMH', true, 'VERIFIED', '+912224177000', 'INTERNAL_REGISTRY', 'HOSPITAL'),
    (gen_random_uuid(), 'Lilavati Hospital and Research Centre', 'A-791, Bandra Reclamation, Bandra West', 'Mumbai', 'Maharashtra', '400050', 19.0514, 72.8291, 'PLACE-IN-BOM-LILAVATI', true, 'VERIFIED', '+912226751000', 'INTERNAL_REGISTRY', 'HOSPITAL'),
    (gen_random_uuid(), 'Nizam''s Institute of Medical Sciences', 'Punjagutta Road', 'Hyderabad', 'Telangana', '500082', 17.4222, 78.4529, 'PLACE-IN-HYD-NIMS', true, 'VERIFIED', '+914023489000', 'INTERNAL_REGISTRY', 'HOSPITAL')
ON CONFLICT (place_id) DO NOTHING;
