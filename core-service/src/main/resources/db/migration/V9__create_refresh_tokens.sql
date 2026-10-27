CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    family_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX idx_refresh_tokens_user
    ON refresh_tokens(user_id, created_at DESC);

CREATE INDEX idx_refresh_tokens_family
    ON refresh_tokens(family_id);

CREATE INDEX idx_refresh_tokens_expiry
    ON refresh_tokens(expires_at);
