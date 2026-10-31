-- Reset rental data linked to the Roomix demo users and rooms.
-- Executed by scripts/reset-demo-data.ps1 against rental_db.

BEGIN;

CREATE TEMP TABLE demo_user_ids (id UUID PRIMARY KEY) ON COMMIT DROP;
INSERT INTO demo_user_ids (id)
SELECT ('00000000-0000-0000-0000-' || lpad(seed::text, 12, '0'))::uuid
FROM generate_series(1, 19) AS seed;

CREATE TEMP TABLE demo_room_ids (id UUID PRIMARY KEY) ON COMMIT DROP;
INSERT INTO demo_room_ids (id)
SELECT ('00000000-0000-0000-0000-' || lpad(seed::text, 12, '0'))::uuid
FROM generate_series(31, 72) AS seed;

CREATE TEMP TABLE demo_contract_ids (id UUID PRIMARY KEY) ON COMMIT DROP;
INSERT INTO demo_contract_ids (id)
SELECT id FROM contracts
WHERE room_id IN (SELECT id FROM demo_room_ids)
   OR tenant_id IN (SELECT id FROM demo_user_ids)
   OR landlord_id IN (SELECT id FROM demo_user_ids);

CREATE TEMP TABLE demo_request_ids (id UUID PRIMARY KEY) ON COMMIT DROP;
INSERT INTO demo_request_ids (id)
SELECT id FROM rental_requests
WHERE room_id IN (SELECT id FROM demo_room_ids)
   OR tenant_id IN (SELECT id FROM demo_user_ids)
   OR landlord_id IN (SELECT id FROM demo_user_ids);

CREATE TEMP TABLE demo_invoice_ids (id UUID PRIMARY KEY) ON COMMIT DROP;
INSERT INTO demo_invoice_ids (id)
SELECT id FROM invoices
WHERE contract_id IN (SELECT id FROM demo_contract_ids);

CREATE TEMP TABLE demo_payment_ids (id UUID PRIMARY KEY) ON COMMIT DROP;
INSERT INTO demo_payment_ids (id)
SELECT id FROM vnpay_payment_attempts
WHERE invoice_id IN (SELECT id FROM demo_invoice_ids);

DELETE FROM audit_logs
WHERE user_id IN (SELECT id FROM demo_user_ids)
   OR entity_id IN (SELECT id::text FROM demo_contract_ids)
   OR entity_id IN (SELECT id::text FROM demo_invoice_ids)
   OR entity_id IN (SELECT id::text FROM demo_payment_ids);

DELETE FROM contact_cards
WHERE rental_request_id IN (SELECT id FROM demo_request_ids);

DELETE FROM tenancy_settlements
WHERE contract_id IN (SELECT id FROM demo_contract_ids);

DELETE FROM invoice_corrections
WHERE invoice_id IN (SELECT id FROM demo_invoice_ids);

DELETE FROM invoice_reminders
WHERE invoice_id IN (SELECT id FROM demo_invoice_ids);

DELETE FROM vnpay_payment_attempts
WHERE invoice_id IN (SELECT id FROM demo_invoice_ids);

DELETE FROM invoice_items
WHERE invoice_id IN (SELECT id FROM demo_invoice_ids);

DELETE FROM invoices
WHERE id IN (SELECT id FROM demo_invoice_ids);

DELETE FROM meter_readings
WHERE contract_id IN (SELECT id FROM demo_contract_ids);

DELETE FROM active_tenancies
WHERE room_id IN (SELECT id FROM demo_room_ids)
   OR tenant_id IN (SELECT id FROM demo_user_ids)
   OR contract_id IN (SELECT id FROM demo_contract_ids);

DELETE FROM tenancy_events
WHERE room_id IN (SELECT id FROM demo_room_ids)
   OR tenancy_id IN (SELECT id FROM demo_contract_ids);

DELETE FROM contracts
WHERE id IN (SELECT id FROM demo_contract_ids);

DELETE FROM rental_requests
WHERE id IN (SELECT id FROM demo_request_ids);

DELETE FROM room_booking_locks
WHERE room_id IN (SELECT id FROM demo_room_ids);

COMMIT;
