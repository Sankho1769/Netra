-- Migration V7: Create Blood Bank Accounts Table
-- Links authenticated users (ROLE_BLOODBANK) to specific blood banks for granular ownership authorization.

CREATE TABLE blood_bank_accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blood_bank_id UUID NOT NULL REFERENCES blood_banks(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_blood_bank_account_user_bank UNIQUE (user_id, blood_bank_id)
);

CREATE INDEX idx_blood_bank_accounts_user_bank ON blood_bank_accounts(user_id, blood_bank_id);
CREATE INDEX idx_blood_bank_accounts_user_id ON blood_bank_accounts(user_id);
CREATE INDEX idx_blood_bank_accounts_blood_bank_id ON blood_bank_accounts(blood_bank_id);
CREATE INDEX idx_blood_bank_accounts_status ON blood_bank_accounts(status);
