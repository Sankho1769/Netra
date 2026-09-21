-- ==========================================================
-- NETRA Platform: Notifications & Device Token Management Schema
-- Version: V13 (PostgreSQL / Supabase compatible)
-- Note on UUID default: Uses built-in gen_random_uuid() available natively in PostgreSQL 13+ and Supabase.
-- ==========================================================

-- 1. Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id UUID NOT NULL,
    type VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    reference_type VARCHAR(64),
    reference_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP WITH TIME ZONE,
    delivery_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_notifications_recipient
        FOREIGN KEY (recipient_user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_notifications_delivery_status
        CHECK (delivery_status IN ('PENDING', 'SENT', 'FAILED')),

    CONSTRAINT chk_notifications_type
        CHECK (type IN ('MATCH_CREATED', 'MATCH_ACCEPTED', 'MATCH_DECLINED', 'MATCH_EXPIRED', 'BLOOD_REQUEST_CANCELLED', 'EMERGENCY_REQUEST_CREATED')),

    CONSTRAINT uq_notifications_idempotency_key
        UNIQUE (idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_created ON notifications(recipient_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_unread ON notifications(recipient_user_id, read_at);
CREATE INDEX IF NOT EXISTS idx_notifications_reference ON notifications(reference_type, reference_id);

-- 2. User Device Tokens Table (for Push Notification Delivery)
CREATE TABLE IF NOT EXISTS user_device_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token VARCHAR(512) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    provider VARCHAR(32) NOT NULL DEFAULT 'FCM',
    platform VARCHAR(32) NOT NULL DEFAULT 'ANDROID',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_user_device_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_device_tokens_platform
        CHECK (platform IN ('ANDROID', 'IOS', 'WEB')),

    CONSTRAINT uq_user_device_tokens_token
        UNIQUE (token)
);

CREATE INDEX IF NOT EXISTS idx_device_tokens_user_active ON user_device_tokens(user_id, active);
CREATE INDEX IF NOT EXISTS idx_device_tokens_token_hash ON user_device_tokens(token_hash);
