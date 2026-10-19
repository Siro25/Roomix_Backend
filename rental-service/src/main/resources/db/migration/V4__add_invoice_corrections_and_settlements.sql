ALTER TABLE invoices ADD COLUMN revision INTEGER NOT NULL DEFAULT 0 CHECK (revision >= 0);

CREATE TABLE invoice_corrections (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    revision INTEGER NOT NULL CHECK (revision > 0),
    corrected_by UUID NOT NULL,
    reason VARCHAR(2000) NOT NULL CHECK (length(trim(reason)) > 0),
    old_values JSONB NOT NULL,
    new_values JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (invoice_id, revision)
);

CREATE TABLE tenancy_settlements (
    id UUID PRIMARY KEY,
    contract_id UUID NOT NULL UNIQUE REFERENCES contracts(id),
    actual_return_date DATE NOT NULL,
    last_billed_month VARCHAR(7),
    unbilled_months INTEGER NOT NULL CHECK (unbilled_months >= 0),
    previous_electricity NUMERIC(15,2) NOT NULL,
    final_electricity NUMERIC(15,2) NOT NULL,
    previous_water NUMERIC(15,2) NOT NULL,
    final_water NUMERIC(15,2) NOT NULL,
    monthly_rent NUMERIC(15,2) NOT NULL,
    electricity_unit_price NUMERIC(15,2) NOT NULL,
    water_unit_price NUMERIC(15,2) NOT NULL,
    invoice_debt NUMERIC(19,2) NOT NULL,
    additional_rent NUMERIC(19,2) NOT NULL,
    additional_electricity NUMERIC(19,2) NOT NULL,
    additional_water NUMERIC(19,2) NOT NULL,
    damage_charge NUMERIC(15,2) NOT NULL,
    damage_reason VARCHAR(2000),
    total_due NUMERIC(19,2) NOT NULL,
    deposit_amount NUMERIC(19,2) NOT NULL,
    deposit_applied NUMERIC(19,2) NOT NULL,
    amount_to_collect NUMERIC(19,2) NOT NULL,
    amount_to_refund NUMERIC(19,2) NOT NULL,
    collected_amount NUMERIC(19,2) NOT NULL,
    refunded_amount NUMERIC(19,2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL CHECK (payment_method IN ('CASH', 'BANK_TRANSFER')),
    settled_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    invoice_balances JSONB NOT NULL,
    CONSTRAINT ck_settlement_readings CHECK (
        previous_electricity >= 0 AND final_electricity >= previous_electricity
        AND previous_water >= 0 AND final_water >= previous_water),
    CONSTRAINT ck_settlement_charges CHECK (
        monthly_rent > 0 AND electricity_unit_price >= 0 AND water_unit_price >= 0
        AND invoice_debt >= 0 AND damage_charge >= 0
        AND additional_rent = monthly_rent * unbilled_months
        AND additional_electricity = round((final_electricity - previous_electricity) * electricity_unit_price, 2)
        AND additional_water = round((final_water - previous_water) * water_unit_price, 2)
        AND total_due = invoice_debt + additional_rent + additional_electricity + additional_water + damage_charge),
    CONSTRAINT ck_settlement_funds CHECK (
        deposit_amount >= 0 AND deposit_applied = least(deposit_amount, total_due)
        AND amount_to_collect = total_due - deposit_applied
        AND amount_to_refund = deposit_amount - deposit_applied
        AND collected_amount = amount_to_collect AND refunded_amount = amount_to_refund),
    CONSTRAINT ck_damage_reason CHECK (
        damage_charge = 0 OR (damage_reason IS NOT NULL AND length(trim(damage_reason)) > 0))
);
