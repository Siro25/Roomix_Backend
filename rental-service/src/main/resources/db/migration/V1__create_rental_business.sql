-- UUID references to Core belong to another database and intentionally have no cross-database FK.
-- State names follow the current rental requirements, superseding the older database note.
CREATE TABLE room_booking_locks (room_id UUID PRIMARY KEY);

CREATE TABLE rental_requests (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL,
    post_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    landlord_id UUID NOT NULL,
    note VARCHAR(2000),
    status VARCHAR(20) NOT NULL,
    reject_reason VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE contracts (
    id UUID PRIMARY KEY,
    contract_code VARCHAR(50) NOT NULL UNIQUE,
    rental_request_id UUID NOT NULL UNIQUE,
    room_id UUID NOT NULL,
    landlord_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    actual_return_date DATE,
    monthly_rent NUMERIC(15,2) NOT NULL,
    deposit_amount NUMERIC(15,2) NOT NULL,
    electricity_unit_price NUMERIC(15,2) NOT NULL,
    water_unit_price NUMERIC(15,2) NOT NULL,
    initial_electricity NUMERIC(15,2) NOT NULL,
    initial_water NUMERIC(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    terms_and_conditions VARCHAR(5000),
    cancellation_reason VARCHAR(2000),
    deposit_confirmed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE active_tenancies (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL UNIQUE,
    contract_id UUID NOT NULL UNIQUE,
    tenant_id UUID NOT NULL,
    check_in_date DATE NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE meter_readings (
    id UUID PRIMARY KEY,
    contract_id UUID NOT NULL,
    room_id UUID NOT NULL,
    billing_month VARCHAR(7) NOT NULL,
    old_electricity_val NUMERIC(15,2) NOT NULL,
    new_electricity_val NUMERIC(15,2) NOT NULL,
    electricity_usage NUMERIC(15,2) NOT NULL,
    old_water_val NUMERIC(15,2) NOT NULL,
    new_water_val NUMERIC(15,2) NOT NULL,
    water_usage NUMERIC(15,2) NOT NULL,
    recorded_by UUID NOT NULL,
    reading_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    invoice_code VARCHAR(50) NOT NULL UNIQUE,
    contract_id UUID NOT NULL,
    meter_reading_id UUID NOT NULL UNIQUE,
    billing_month VARCHAR(7) NOT NULL,
    due_date DATE NOT NULL,
    total_amount NUMERIC(19,2) NOT NULL,
    paid_amount NUMERIC(19,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    is_disputed BOOLEAN NOT NULL,
    dispute_reason VARCHAR(2000),
    dispute_resolution VARCHAR(2000),
    confirmed_at TIMESTAMPTZ,
    payment_date TIMESTAMPTZ,
    payment_method VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE invoice_items (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    item_type VARCHAR(20) NOT NULL,
    quantity NUMERIC(15,2) NOT NULL,
    unit_price NUMERIC(15,2) NOT NULL,
    total_price NUMERIC(19,2) NOT NULL
);

ALTER TABLE rental_requests ADD CONSTRAINT ck_request_status
    CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED'));
CREATE UNIQUE INDEX uq_pending_request ON rental_requests(room_id, tenant_id) WHERE status = 'PENDING';
CREATE INDEX ix_request_landlord ON rental_requests(landlord_id, created_at DESC);
CREATE INDEX ix_request_tenant ON rental_requests(tenant_id, created_at DESC);

ALTER TABLE contracts ADD CONSTRAINT fk_contract_request FOREIGN KEY (rental_request_id) REFERENCES rental_requests(id);
ALTER TABLE contracts ADD CONSTRAINT ck_contract_status
    CHECK (status IN ('PENDING', 'ACTIVE', 'ENDED', 'CANCELLED'));
ALTER TABLE contracts ADD CONSTRAINT ck_contract_dates CHECK (end_date > start_date);
ALTER TABLE contracts ADD CONSTRAINT ck_contract_amounts CHECK (
    monthly_rent > 0 AND deposit_amount >= 0 AND electricity_unit_price >= 0 AND water_unit_price >= 0
    AND initial_electricity >= 0 AND initial_water >= 0);
CREATE UNIQUE INDEX uq_room_open_contract ON contracts(room_id) WHERE status IN ('PENDING', 'ACTIVE');
CREATE INDEX ix_contract_landlord ON contracts(landlord_id, created_at DESC);
CREATE INDEX ix_contract_tenant ON contracts(tenant_id, created_at DESC);

ALTER TABLE active_tenancies ADD CONSTRAINT fk_active_contract FOREIGN KEY (contract_id) REFERENCES contracts(id);
ALTER TABLE meter_readings ADD CONSTRAINT fk_reading_contract FOREIGN KEY (contract_id) REFERENCES contracts(id);
ALTER TABLE meter_readings ADD CONSTRAINT uq_contract_reading_month UNIQUE (contract_id, billing_month);
ALTER TABLE meter_readings ADD CONSTRAINT ck_meter_values CHECK (
    old_electricity_val >= 0 AND new_electricity_val >= old_electricity_val
    AND old_water_val >= 0 AND new_water_val >= old_water_val
    AND electricity_usage = new_electricity_val - old_electricity_val
    AND water_usage = new_water_val - old_water_val);
ALTER TABLE invoices ADD CONSTRAINT fk_invoice_contract FOREIGN KEY (contract_id) REFERENCES contracts(id);
ALTER TABLE invoices ADD CONSTRAINT fk_invoice_reading FOREIGN KEY (meter_reading_id) REFERENCES meter_readings(id);
ALTER TABLE invoices ADD CONSTRAINT uq_contract_invoice_month UNIQUE (contract_id, billing_month);
ALTER TABLE invoices ADD CONSTRAINT ck_invoice_status CHECK (status IN ('PENDING', 'CONFIRMED', 'PAID'));
ALTER TABLE invoices ADD CONSTRAINT ck_invoice_amounts CHECK (
    total_amount >= 0 AND paid_amount >= 0 AND paid_amount <= total_amount);
ALTER TABLE invoice_items ADD CONSTRAINT fk_item_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id);
ALTER TABLE invoice_items ADD CONSTRAINT uq_invoice_item_type UNIQUE (invoice_id, item_type);
ALTER TABLE invoice_items ADD CONSTRAINT ck_item_amounts CHECK (
    quantity >= 0 AND unit_price >= 0 AND total_price = round(quantity * unit_price, 2));

-- Transactional outbox only. No Kafka producer, consumer or scheduler is enabled yet.
CREATE TABLE tenancy_events (
    sequence BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    event_type VARCHAR(40) NOT NULL CHECK (event_type IN ('TENANCY_ACTIVATED', 'TENANCY_ENDED')),
    room_id UUID NOT NULL,
    tenancy_id UUID NOT NULL REFERENCES contracts(id),
    partition_key VARCHAR(36) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    CONSTRAINT ck_event_partition_key CHECK (partition_key = room_id::text)
);
CREATE INDEX ix_event_unpublished ON tenancy_events(sequence) WHERE published_at IS NULL;
