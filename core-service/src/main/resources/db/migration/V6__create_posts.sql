CREATE TABLE posts (
    id UUID PRIMARY KEY,
    landlord_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE RESTRICT,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    rental_price NUMERIC(15, 2) NOT NULL,
    deposit_amount NUMERIC(15, 2) NOT NULL,
    available_from DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reject_reason TEXT,
    view_count BIGINT NOT NULL DEFAULT 0,
    approved_by UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_posts_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'HIDDEN')),
    CONSTRAINT ck_posts_rental_price CHECK (rental_price >= 0),
    CONSTRAINT ck_posts_deposit_amount CHECK (deposit_amount >= 0)
);

CREATE INDEX idx_posts_status_created ON posts(status, created_at DESC);
CREATE INDEX idx_posts_landlord_id ON posts(landlord_id);
CREATE INDEX idx_posts_room_id ON posts(room_id);
