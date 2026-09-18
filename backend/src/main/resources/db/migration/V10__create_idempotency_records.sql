-- ============================================================================
-- V10: Create Idempotency Records Schema
-- ============================================================================

CREATE TABLE IF NOT EXISTS idempotency_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    idempotency_key VARCHAR(255) NOT NULL,
    request_fingerprint VARCHAR(255) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id UUID,
    response_status INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uq_idempotency_user_key_resource UNIQUE (user_id, idempotency_key, resource_type)
);

CREATE INDEX IF NOT EXISTS idx_idempotency_expires_at ON idempotency_records(expires_at);
CREATE INDEX IF NOT EXISTS idx_idempotency_lookup ON idempotency_records(user_id, idempotency_key, resource_type);
