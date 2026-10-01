CREATE TABLE floors (
    id UUID PRIMARY KEY,
    house_id UUID NOT NULL,
    floor_number INTEGER NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    CONSTRAINT fk_floors_house FOREIGN KEY (house_id) REFERENCES houses(id) ON DELETE RESTRICT,
    CONSTRAINT uk_floors_house_number UNIQUE (house_id, floor_number),
    CONSTRAINT ck_floors_number CHECK (floor_number >= 0),
    CONSTRAINT ck_floors_name CHECK (length(trim(name)) > 0)
);

-- No floors existed before this migration; align the derived count with the new table.
UPDATE houses SET total_floors = 0;
