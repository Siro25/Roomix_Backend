ALTER TABLE contracts ADD COLUMN deposit_deadline TIMESTAMPTZ;
-- Give existing pending reservations a full grace period at deployment.
UPDATE contracts SET deposit_deadline = CASE WHEN status = 'PENDING'
    THEN CURRENT_TIMESTAMP + INTERVAL '24 hours' ELSE created_at + INTERVAL '24 hours' END;
ALTER TABLE contracts ALTER COLUMN deposit_deadline SET NOT NULL;
CREATE INDEX ix_pending_deposit_deadline ON contracts(deposit_deadline) WHERE status = 'PENDING';
