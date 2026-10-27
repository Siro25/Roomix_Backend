-- Reset only data that belongs to the fixed Roomix demo accounts and rooms.
-- This file is executed by scripts/reset-demo-data.ps1 against core_db.

BEGIN;

DELETE FROM processed_rental_events
WHERE room_id IN (
    '00000000-0000-0000-0000-000000000031',
    '00000000-0000-0000-0000-000000000032'
);

DELETE FROM favorites
WHERE tenant_id = '00000000-0000-0000-0000-000000000003'
   OR post_id IN (
       SELECT id FROM posts
       WHERE landlord_id = '00000000-0000-0000-0000-000000000002'
   );

DELETE FROM room_images
WHERE room_id IN (
    SELECT room.id
    FROM rooms room
    JOIN floors floor ON floor.id = room.floor_id
    JOIN houses house ON house.id = floor.house_id
    WHERE house.landlord_id = '00000000-0000-0000-0000-000000000002'
);

DELETE FROM posts
WHERE landlord_id = '00000000-0000-0000-0000-000000000002';

DELETE FROM rooms
WHERE floor_id IN (
    SELECT floor.id
    FROM floors floor
    JOIN houses house ON house.id = floor.house_id
    WHERE house.landlord_id = '00000000-0000-0000-0000-000000000002'
);

DELETE FROM floors
WHERE house_id IN (
    SELECT id FROM houses
    WHERE landlord_id = '00000000-0000-0000-0000-000000000002'
);

DELETE FROM houses
WHERE landlord_id = '00000000-0000-0000-0000-000000000002';

DELETE FROM notifications
WHERE user_id IN (
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000003'
);

DELETE FROM audit_logs
WHERE user_id IN (
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000003'
);

DELETE FROM password_reset_tokens
WHERE user_id IN (
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000003'
);

DELETE FROM refresh_tokens
WHERE user_id IN (
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000003'
);

DELETE FROM violation_reports
WHERE reporter_id IN (
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000003'
)
   OR reviewed_by IN (
       '00000000-0000-0000-0000-000000000001',
       '00000000-0000-0000-0000-000000000002',
       '00000000-0000-0000-0000-000000000003'
   );

COMMIT;
