-- Reset rental data created with the fixed Roomix demo users and rooms.
-- This file is executed by scripts/reset-demo-data.ps1 against rental_db.

BEGIN;

DELETE FROM contact_cards
WHERE rental_request_id IN (
    SELECT id
    FROM rental_requests
    WHERE room_id IN (
        '00000000-0000-0000-0000-000000000031',
        '00000000-0000-0000-0000-000000000032'
    )
       OR tenant_id = '00000000-0000-0000-0000-000000000003'
       OR landlord_id = '00000000-0000-0000-0000-000000000002'
);

DELETE FROM tenancy_settlements
WHERE contract_id IN (
    SELECT id
    FROM contracts
    WHERE room_id IN (
        '00000000-0000-0000-0000-000000000031',
        '00000000-0000-0000-0000-000000000032'
    )
       OR tenant_id = '00000000-0000-0000-0000-000000000003'
       OR landlord_id = '00000000-0000-0000-0000-000000000002'
);

DELETE FROM invoice_corrections
WHERE invoice_id IN (
    SELECT invoice.id
    FROM invoices invoice
    JOIN contracts contract ON contract.id = invoice.contract_id
    WHERE contract.room_id IN (
        '00000000-0000-0000-0000-000000000031',
        '00000000-0000-0000-0000-000000000032'
    )
       OR contract.tenant_id = '00000000-0000-0000-0000-000000000003'
       OR contract.landlord_id = '00000000-0000-0000-0000-000000000002'
);

DELETE FROM invoice_reminders
WHERE invoice_id IN (
    SELECT invoice.id
    FROM invoices invoice
    JOIN contracts contract ON contract.id = invoice.contract_id
    WHERE contract.room_id IN (
        '00000000-0000-0000-0000-000000000031',
        '00000000-0000-0000-0000-000000000032'
    )
       OR contract.tenant_id = '00000000-0000-0000-0000-000000000003'
       OR contract.landlord_id = '00000000-0000-0000-0000-000000000002'
);

DELETE FROM vnpay_payment_attempts
WHERE invoice_id IN (
    SELECT invoice.id
    FROM invoices invoice
    JOIN contracts contract ON contract.id = invoice.contract_id
    WHERE contract.room_id IN (
        '00000000-0000-0000-0000-000000000031',
        '00000000-0000-0000-0000-000000000032'
    )
       OR contract.tenant_id = '00000000-0000-0000-0000-000000000003'
       OR contract.landlord_id = '00000000-0000-0000-0000-000000000002'
);

DELETE FROM invoice_items
WHERE invoice_id IN (
    SELECT invoice.id
    FROM invoices invoice
    JOIN contracts contract ON contract.id = invoice.contract_id
    WHERE contract.room_id IN (
        '00000000-0000-0000-0000-000000000031',
        '00000000-0000-0000-0000-000000000032'
    )
       OR contract.tenant_id = '00000000-0000-0000-0000-000000000003'
       OR contract.landlord_id = '00000000-0000-0000-0000-000000000002'
);

DELETE FROM invoices
WHERE contract_id IN (
    SELECT id
    FROM contracts
    WHERE room_id IN (
        '00000000-0000-0000-0000-000000000031',
        '00000000-0000-0000-0000-000000000032'
    )
       OR tenant_id = '00000000-0000-0000-0000-000000000003'
       OR landlord_id = '00000000-0000-0000-0000-000000000002'
);

DELETE FROM meter_readings
WHERE contract_id IN (
    SELECT id
    FROM contracts
    WHERE room_id IN (
        '00000000-0000-0000-0000-000000000031',
        '00000000-0000-0000-0000-000000000032'
    )
       OR tenant_id = '00000000-0000-0000-0000-000000000003'
       OR landlord_id = '00000000-0000-0000-0000-000000000002'
);

DELETE FROM active_tenancies
WHERE room_id IN (
    '00000000-0000-0000-0000-000000000031',
    '00000000-0000-0000-0000-000000000032'
)
   OR tenant_id = '00000000-0000-0000-0000-000000000003'
   OR contract_id IN (
       SELECT id
       FROM contracts
       WHERE landlord_id = '00000000-0000-0000-0000-000000000002'
   );

DELETE FROM tenancy_events
WHERE room_id IN (
    '00000000-0000-0000-0000-000000000031',
    '00000000-0000-0000-0000-000000000032'
)
   OR tenancy_id IN (
       SELECT id
       FROM contracts
       WHERE tenant_id = '00000000-0000-0000-0000-000000000003'
          OR landlord_id = '00000000-0000-0000-0000-000000000002'
   );

DELETE FROM contracts
WHERE room_id IN (
    '00000000-0000-0000-0000-000000000031',
    '00000000-0000-0000-0000-000000000032'
)
   OR tenant_id = '00000000-0000-0000-0000-000000000003'
   OR landlord_id = '00000000-0000-0000-0000-000000000002';

DELETE FROM rental_requests
WHERE room_id IN (
    '00000000-0000-0000-0000-000000000031',
    '00000000-0000-0000-0000-000000000032'
)
   OR tenant_id = '00000000-0000-0000-0000-000000000003'
   OR landlord_id = '00000000-0000-0000-0000-000000000002';

DELETE FROM room_booking_locks
WHERE room_id IN (
    '00000000-0000-0000-0000-000000000031',
    '00000000-0000-0000-0000-000000000032'
);

COMMIT;
