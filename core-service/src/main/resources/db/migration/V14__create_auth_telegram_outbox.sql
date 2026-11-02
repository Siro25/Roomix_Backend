-- Thông báo chữ của Core; không lưu ảnh, password, JWT hoặc completionToken.
CREATE TABLE IF NOT EXISTS auth_telegram_outbox (
    id UUID PRIMARY KEY,
    event_key VARCHAR(128) NOT NULL UNIQUE,
    event_type VARCHAR(40) NOT NULL CHECK (event_type IN (
        'PASSWORD_LOGIN_SUCCEEDED', 'PASSWORD_LOGIN_FAILED', 'FACE_LOGIN_SUCCEEDED')),
    message TEXT,
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts BETWEEN 0 AND 5),
    created_at TIMESTAMPTZ NOT NULL,
    available_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    CHECK ((status = 'PENDING' AND message IS NOT NULL) OR (status <> 'PENDING' AND message IS NULL))
);
CREATE INDEX IF NOT EXISTS auth_telegram_pending_idx ON auth_telegram_outbox (available_at) WHERE status = 'PENDING';
