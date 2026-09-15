-- ==========================================================
-- NETRA Platform: Blood Banks & Blood Inventory Schema
-- Version: V6 (PostgreSQL / Supabase compatible)
-- Note: Strict integrity for verified blood centres and stock.
-- Enforces non-negative units at the database level.
-- Supports optimistic concurrency locking and spatial coordinates.
-- ==========================================================

-- 1. Blood Banks table
CREATE TABLE IF NOT EXISTS blood_banks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    registration_number VARCHAR(64),
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    phone VARCHAR(32) NOT NULL,
    email VARCHAR(255),
    verification_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    operating_status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_blood_banks_verification CHECK (verification_status IN ('PENDING', 'VERIFIED', 'SUSPENDED', 'REJECTED')),
    CONSTRAINT chk_blood_banks_operating CHECK (operating_status IN ('OPEN', 'CLOSED', 'TEMPORARILY_UNAVAILABLE')),
    CONSTRAINT chk_blood_banks_lat CHECK (latitude >= -90.0 AND latitude <= 90.0),
    CONSTRAINT chk_blood_banks_lng CHECK (longitude >= -180.0 AND longitude <= 180.0)
);

-- 2. Blood Inventory table
CREATE TABLE IF NOT EXISTS blood_inventory (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    blood_bank_id UUID NOT NULL REFERENCES blood_banks(id) ON DELETE CASCADE,
    blood_group VARCHAR(8) NOT NULL,
    units_available INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    last_updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_blood_inventory_group CHECK (blood_group IN ('A+', 'A-', 'B+', 'B-', 'O+', 'O-', 'AB+', 'AB-')),
    CONSTRAINT chk_blood_inventory_units CHECK (units_available >= 0),
    CONSTRAINT uq_blood_inventory_bank_group UNIQUE (blood_bank_id, blood_group)
);

-- 3. Indexes for discovery, filtering and nearby queries
CREATE INDEX IF NOT EXISTS idx_blood_banks_city ON blood_banks(city);
CREATE INDEX IF NOT EXISTS idx_blood_banks_verification_status ON blood_banks(verification_status);
CREATE INDEX IF NOT EXISTS idx_blood_banks_operating_status ON blood_banks(operating_status);
CREATE INDEX IF NOT EXISTS idx_blood_banks_coords ON blood_banks(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_blood_inventory_bank_id ON blood_inventory(blood_bank_id);
CREATE INDEX IF NOT EXISTS idx_blood_inventory_blood_group ON blood_inventory(blood_group);
