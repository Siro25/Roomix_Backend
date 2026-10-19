ALTER TABLE tenancy_events
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN last_error VARCHAR(1000);

CREATE INDEX ix_event_room_unpublished
    ON tenancy_events(room_id, sequence) WHERE published_at IS NULL;
