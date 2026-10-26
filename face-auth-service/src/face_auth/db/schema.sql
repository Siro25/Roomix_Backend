-- Database này chỉ thuộc Face Service, không chứa tài khoản của Core.
CREATE TABLE IF NOT EXISTS face_profiles (
    admin_id TEXT PRIMARY KEY CHECK (admin_id ~ '^([1-9][0-9]{0,18}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})$'),
    embedding JSONB NOT NULL CHECK (jsonb_typeof(embedding) = 'array'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS face_challenges (
    id TEXT PRIMARY KEY,
    auth_session_id TEXT NOT NULL UNIQUE,
    state JSONB NOT NULL CHECK (jsonb_typeof(state) = 'object'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS face_challenges_expires_idx
    ON face_challenges (((state->>'expiresAt')::double precision))
    WHERE state->>'status' NOT IN ('DONE', 'EXPIRED');

-- Hàng đợi gửi ảnh, ghi cùng transaction với kết quả Face Auth.
CREATE TABLE IF NOT EXISTS telegram_outbox (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    auth_session_id TEXT NOT NULL,
    event_type TEXT NOT NULL CHECK (event_type IN ('FACE_LOGIN_FAILED', 'FACE_ENROLLED')),
    admin_id TEXT,
    reason TEXT,
    photo BYTEA,
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts BETWEEN 0 AND 5),
    created_at TIMESTAMPTZ NOT NULL,
    available_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    UNIQUE (auth_session_id, event_type),
    CHECK ((status = 'PENDING' AND photo IS NOT NULL)
        OR (status IN ('SENT', 'FAILED') AND photo IS NULL))
);

CREATE INDEX IF NOT EXISTS telegram_outbox_pending_idx
    ON telegram_outbox (available_at) WHERE status = 'PENDING';
