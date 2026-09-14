-- ==========================================================
-- NETRA Platform: Blood Donation Eligibility Database Schema
-- Version: V1 (PostgreSQL / Supabase compatible)
-- Note: Designed with strict healthcare data minimization and auditability.
-- ==========================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Eligibility Questions
CREATE TABLE IF NOT EXISTS eligibility_questions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    version VARCHAR(32) NOT NULL,
    question_key VARCHAR(64) NOT NULL,
    step_number INT NOT NULL,
    category VARCHAR(64) NOT NULL,
    question_text TEXT NOT NULL,
    help_text TEXT,
    question_type VARCHAR(32) NOT NULL,
    options_json TEXT,
    validation_json TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_question_version_key UNIQUE (version, question_key)
);

-- 2. Eligibility Rules
CREATE TABLE IF NOT EXISTS eligibility_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    rule_id VARCHAR(64) NOT NULL,
    version VARCHAR(32) NOT NULL,
    description TEXT NOT NULL,
    source VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL,
    condition_expression TEXT NOT NULL,
    result_type VARCHAR(32) NOT NULL,
    deferral_duration_days INT,
    explanation TEXT NOT NULL,
    severity VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to TIMESTAMP WITH TIME ZONE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_rule_version_id UNIQUE (version, rule_id)
);

-- 3. Deferral Reasons
CREATE TABLE IF NOT EXISTS deferral_reasons (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    rule_id UUID NOT NULL REFERENCES eligibility_rules(id) ON DELETE CASCADE,
    reason_code VARCHAR(64) NOT NULL,
    display_text TEXT NOT NULL,
    recommended_action TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Eligibility Sessions
CREATE TABLE IF NOT EXISTS eligibility_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID,
    rule_version VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
    result VARCHAR(32),
    estimated_eligible_date DATE,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. Eligibility Answers
CREATE TABLE IF NOT EXISTS eligibility_answers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL REFERENCES eligibility_sessions(id) ON DELETE CASCADE,
    question_key VARCHAR(64) NOT NULL,
    answer_value TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_session_question UNIQUE (session_id, question_key)
);

-- 6. Privacy-Safe Audit Logs (Zero Health Answers Logged)
CREATE TABLE IF NOT EXISTS eligibility_audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_type VARCHAR(64) NOT NULL,
    user_id UUID,
    session_id UUID,
    rule_version VARCHAR(32) NOT NULL,
    result_type VARCHAR(32),
    ip_hash VARCHAR(64),
    user_agent VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON eligibility_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_expires_at ON eligibility_sessions(expires_at);
CREATE INDEX IF NOT EXISTS idx_answers_session_id ON eligibility_answers(session_id);
CREATE INDEX IF NOT EXISTS idx_questions_active_version ON eligibility_questions(version, active, sort_order);
CREATE INDEX IF NOT EXISTS idx_rules_version_active ON eligibility_rules(version, active);
