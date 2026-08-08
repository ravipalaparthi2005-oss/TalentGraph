-- ==============================================================================
-- TalentGraph Phase 03 Authentication & Identity Schema
-- Version: 2.0.0
-- Migration: V2__authentication.sql
-- Description: User identities (OAuth providers) and hashed refresh tokens.
-- ==============================================================================

-- 1. USER IDENTITIES TABLE (Google OAuth2 & External Providers)
CREATE TABLE user_identities (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_provider_subject UNIQUE (provider, provider_subject)
);
CREATE INDEX idx_user_identities_user ON user_identities(user_id);

-- 2. REFRESH TOKENS TABLE (Session Management & Token Rotation)
CREATE TABLE refresh_tokens (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    replaced_by_token_id UUID REFERENCES refresh_tokens(id) ON DELETE SET NULL,
    created_ip VARCHAR(100),
    user_agent VARCHAR(500)
);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
