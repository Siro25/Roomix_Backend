-- Reset data owned by the fixed Roomix demo users.
-- Executed by scripts/reset-demo-data.ps1 against core_db.

BEGIN;

CREATE TEMP TABLE demo_user_ids (id UUID PRIMARY KEY) ON COMMIT DROP;
INSERT INTO demo_user_ids (id)
SELECT ('00000000-0000-0000-0000-' || lpad(seed::text, 12, '0'))::uuid
FROM generate_series(1, 19) AS seed;

CREATE TEMP TABLE demo_landlord_ids (id UUID PRIMARY KEY) ON COMMIT DROP;
INSERT INTO demo_landlord_ids (id) VALUES
('00000000-0000-0000-0000-000000000002'),
('00000000-0000-0000-0000-000000000004'),
('00000000-0000-0000-0000-000000000005'),
('00000000-0000-0000-0000-000000000006'),
('00000000-0000-0000-0000-000000000007');

DELETE FROM processed_rental_events
WHERE room_id IN (
    SELECT room.id
    FROM rooms room
    JOIN floors floor ON floor.id = room.floor_id
    JOIN houses house ON house.id = floor.house_id
    WHERE house.landlord_id IN (SELECT id FROM demo_landlord_ids)
);

DELETE FROM favorites
WHERE tenant_id IN (SELECT id FROM demo_user_ids)
   OR post_id IN (
       SELECT id FROM posts
       WHERE landlord_id IN (SELECT id FROM demo_landlord_ids)
   );

DELETE FROM violation_reports
WHERE reporter_id IN (SELECT id FROM demo_user_ids)
   OR reviewed_by IN (SELECT id FROM demo_user_ids);

DELETE FROM room_images
WHERE room_id IN (
    SELECT room.id
    FROM rooms room
    JOIN floors floor ON floor.id = room.floor_id
    JOIN houses house ON house.id = floor.house_id
    WHERE house.landlord_id IN (SELECT id FROM demo_landlord_ids)
);

DELETE FROM posts
WHERE landlord_id IN (SELECT id FROM demo_landlord_ids);

DELETE FROM rooms
WHERE floor_id IN (
    SELECT floor.id
    FROM floors floor
    JOIN houses house ON house.id = floor.house_id
    WHERE house.landlord_id IN (SELECT id FROM demo_landlord_ids)
);

DELETE FROM floors
WHERE house_id IN (
    SELECT id FROM houses
    WHERE landlord_id IN (SELECT id FROM demo_landlord_ids)
);

DELETE FROM houses
WHERE landlord_id IN (SELECT id FROM demo_landlord_ids);

DELETE FROM notifications
WHERE user_id IN (SELECT id FROM demo_user_ids);

DELETE FROM audit_logs
WHERE user_id IN (SELECT id FROM demo_user_ids);

DELETE FROM password_reset_tokens
WHERE user_id IN (SELECT id FROM demo_user_ids);

DELETE FROM refresh_tokens
WHERE user_id IN (SELECT id FROM demo_user_ids);

COMMIT;
