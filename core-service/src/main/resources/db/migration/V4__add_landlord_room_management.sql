ALTER TABLE rooms
    ALTER COLUMN area DROP NOT NULL,
    ALTER COLUMN base_price DROP NOT NULL,
    ADD COLUMN is_rented BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE posts
    ALTER COLUMN rental_price DROP NOT NULL,
    ALTER COLUMN status SET DEFAULT 'DRAFT';

ALTER TABLE posts DROP CONSTRAINT ck_posts_status;
ALTER TABLE posts ADD CONSTRAINT ck_posts_status
    CHECK (status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'HIDDEN'));

CREATE TABLE room_images (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    image_url VARCHAR(1000) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL DEFAULT 0,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_room_images_room_order
    ON room_images(room_id, display_order);
