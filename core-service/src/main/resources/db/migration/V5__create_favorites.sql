CREATE TABLE favorites (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_favorites_tenant_post UNIQUE (tenant_id, post_id)
);

CREATE INDEX idx_favorites_tenant_created
    ON favorites(tenant_id, created_at DESC);

CREATE INDEX idx_favorites_post_id
    ON favorites(post_id);
