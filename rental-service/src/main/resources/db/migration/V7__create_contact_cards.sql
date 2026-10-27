CREATE TABLE contact_cards (
    id UUID PRIMARY KEY,
    rental_request_id UUID NOT NULL UNIQUE,
    landlord_id UUID NOT NULL,
    landlord_name VARCHAR(100) NOT NULL,
    landlord_phone VARCHAR(20) NOT NULL,
    landlord_zalo VARCHAR(100) NOT NULL,
    landlord_avatar_url VARCHAR(500),
    note VARCHAR(1000),
    issued_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_contact_card_request
        FOREIGN KEY (rental_request_id) REFERENCES rental_requests(id) ON DELETE CASCADE
);

CREATE INDEX idx_contact_cards_landlord
    ON contact_cards(landlord_id, issued_at DESC);
