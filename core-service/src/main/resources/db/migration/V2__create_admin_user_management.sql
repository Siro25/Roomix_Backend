CREATE TABLE houses (
    id UUID PRIMARY KEY,
    landlord_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    name VARCHAR(150) NOT NULL,
    address_street VARCHAR(255) NOT NULL,
    ward VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    description TEXT,
    total_floors INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_houses_landlord_id ON houses(landlord_id);

CREATE TABLE floors (
    id UUID PRIMARY KEY,
    house_id UUID NOT NULL REFERENCES houses(id) ON DELETE CASCADE,
    floor_number INTEGER NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    CONSTRAINT uk_floors_house_number UNIQUE (house_id, floor_number)
);

CREATE INDEX idx_floors_house_id ON floors(house_id);

CREATE TABLE rooms (
    id UUID PRIMARY KEY,
    floor_id UUID NOT NULL REFERENCES floors(id) ON DELETE RESTRICT,
    room_number VARCHAR(50) NOT NULL,
    area NUMERIC(10, 2) NOT NULL,
    base_price NUMERIC(15, 2) NOT NULL,
    max_tenants INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    has_private_bathroom BOOLEAN NOT NULL DEFAULT FALSE,
    has_air_conditioner BOOLEAN NOT NULL DEFAULT FALSE,
    has_water_heater BOOLEAN NOT NULL DEFAULT FALSE,
    has_balcony BOOLEAN NOT NULL DEFAULT FALSE,
    amenities_description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_rooms_status CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'RESERVED', 'MAINTENANCE')),
    CONSTRAINT uk_rooms_floor_number UNIQUE (floor_id, room_number)
);

CREATE INDEX idx_rooms_floor_id ON rooms(floor_id);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL,
    entity_name VARCHAR(50) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_name, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(30) NOT NULL,
    reference_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_notifications_type CHECK (
        type IN ('SYSTEM', 'INVOICE', 'POST_APPROVAL', 'INTEREST_REQUEST', 'GENERAL')
    )
);

CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at DESC);
