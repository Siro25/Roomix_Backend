CREATE TABLE processed_rental_events (
    event_id UUID PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES rooms(id),
    event_sequence BIGINT NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_processed_room_sequence UNIQUE (room_id, event_sequence)
);

CREATE INDEX ix_processed_rental_room_sequence
    ON processed_rental_events(room_id, event_sequence DESC);
