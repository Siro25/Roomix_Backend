CREATE TABLE rooms (
    id UUID PRIMARY KEY,
    floor_id UUID NOT NULL,
    room_number VARCHAR(50) NOT NULL,
    area NUMERIC(10,2) NOT NULL,
    base_price NUMERIC(15,2) NOT NULL,
    max_tenants INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    has_private_bathroom BOOLEAN NOT NULL DEFAULT FALSE,
    has_air_conditioner BOOLEAN NOT NULL DEFAULT FALSE,
    has_water_heater BOOLEAN NOT NULL DEFAULT FALSE,
    has_balcony BOOLEAN NOT NULL DEFAULT FALSE,
    amenities_description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_rooms_floor FOREIGN KEY (floor_id) REFERENCES floors(id) ON DELETE RESTRICT,
    CONSTRAINT uk_rooms_floor_number UNIQUE (floor_id, room_number),
    CONSTRAINT ck_rooms_number CHECK (length(trim(room_number)) > 0 AND room_number = upper(trim(room_number))),
    CONSTRAINT ck_rooms_area CHECK (area > 0),
    CONSTRAINT ck_rooms_price CHECK (base_price > 0),
    CONSTRAINT ck_rooms_capacity CHECK (max_tenants > 0),
    CONSTRAINT ck_rooms_status CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'RESERVED', 'MAINTENANCE')),
    CONSTRAINT ck_rooms_description CHECK (length(amenities_description) <= 5000)
);

CREATE INDEX idx_rooms_status ON rooms (status);
