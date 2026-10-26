CREATE TABLE vnpay_payment_attempts (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    txn_ref VARCHAR(100) NOT NULL UNIQUE,
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'EXPIRED')),
    payment_url TEXT NOT NULL,
    response_code VARCHAR(10),
    transaction_no VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);

CREATE INDEX ix_vnpay_invoice ON vnpay_payment_attempts(invoice_id, created_at DESC);
CREATE UNIQUE INDEX uq_vnpay_pending_invoice ON vnpay_payment_attempts(invoice_id) WHERE status = 'PENDING';
