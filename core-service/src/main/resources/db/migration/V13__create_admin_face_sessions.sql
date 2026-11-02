-- Chỉ tạo bảng phiên Core. Không chứa ảnh hoặc vector khuôn mặt.
CREATE TABLE IF NOT EXISTS admin_face_sessions (
    auth_session_id VARCHAR(128) PRIMARY KEY,
    purpose VARCHAR(10) NOT NULL CHECK (purpose IN ('LOGIN', 'ENROLL')),
    admin_id UUID,
    completion_hash CHAR(64),
    enrollment_jwt_hash CHAR(64),
    state VARCHAR(12) NOT NULL DEFAULT 'WAITING' CHECK (state IN ('WAITING', 'VERIFIED', 'FAILED')),
    validated_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL,
    result_hash CHAR(64),
    decision_accepted BOOLEAN,
    jwt_issued_at TIMESTAMPTZ,
    jwt_expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK ((purpose = 'LOGIN' AND completion_hash IS NOT NULL AND enrollment_jwt_hash IS NULL)
        OR (purpose = 'ENROLL' AND completion_hash IS NULL AND enrollment_jwt_hash IS NOT NULL AND admin_id IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS admin_face_sessions_expiry_idx ON admin_face_sessions (expires_at);
