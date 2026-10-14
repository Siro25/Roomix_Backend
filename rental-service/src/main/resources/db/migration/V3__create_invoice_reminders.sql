CREATE TABLE invoice_reminders (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    tenant_id UUID NOT NULL,
    landlord_id UUID NOT NULL,
    reminder_date DATE NOT NULL,
    outstanding_amount NUMERIC(19,2) NOT NULL CHECK (outstanding_amount > 0),
    content VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (invoice_id, reminder_date)
);
CREATE INDEX ix_reminder_tenant ON invoice_reminders(tenant_id, created_at DESC);
CREATE INDEX ix_unpaid_invoice_due_date ON invoices(due_date) WHERE paid_amount < total_amount;
