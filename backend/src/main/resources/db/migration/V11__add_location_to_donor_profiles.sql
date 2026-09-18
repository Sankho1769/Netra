-- ==========================================================
-- NETRA Platform: Add Location Coordinates to Donor Profiles
-- Version: V11 (PostgreSQL / Supabase compatible)
-- Note: Adds optional geographic coordinates for proximity matching.
-- Coordinates are nullable. Missing coordinates handled safely.
-- ==========================================================

ALTER TABLE donor_profiles ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE donor_profiles ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_donor_profiles_lat'
    ) THEN
        ALTER TABLE donor_profiles ADD CONSTRAINT chk_donor_profiles_lat
            CHECK (latitude IS NULL OR (latitude >= -90.0 AND latitude <= 90.0));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_donor_profiles_lng'
    ) THEN
        ALTER TABLE donor_profiles ADD CONSTRAINT chk_donor_profiles_lng
            CHECK (longitude IS NULL OR (longitude >= -180.0 AND longitude <= 180.0));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_donor_profiles_coords ON donor_profiles(latitude, longitude);
