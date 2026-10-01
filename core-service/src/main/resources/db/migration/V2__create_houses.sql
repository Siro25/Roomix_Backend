CREATE TABLE houses (
    id UUID PRIMARY KEY,
    landlord_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    name VARCHAR(150) NOT NULL,
    address_street VARCHAR(255) NOT NULL,
    ward VARCHAR(100) NOT NULL,
    district VARCHAR(100),
    city VARCHAR(100) NOT NULL,
    latitude NUMERIC(11,8),
    longitude NUMERIC(11,8),
    description TEXT,
    total_floors INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_houses_name CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_houses_address CHECK (length(trim(address_street)) > 0),
    CONSTRAINT ck_houses_ward CHECK (length(trim(ward)) > 0),
    CONSTRAINT ck_houses_city CHECK (length(trim(city)) > 0),
    CONSTRAINT ck_houses_description CHECK (length(description) <= 5000),
    CONSTRAINT ck_houses_total_floors CHECK (total_floors >= 0),
    CONSTRAINT ck_houses_coordinates CHECK (
        (latitude IS NULL AND longitude IS NULL) OR
        (latitude IS NOT NULL AND longitude IS NOT NULL
            AND latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180)
    )
);

CREATE INDEX idx_houses_landlord_created ON houses (landlord_id, created_at DESC, id);
