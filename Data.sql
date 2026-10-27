-- Roomix local E2E seed data
--
-- Run this file against the core_db database after Flyway has created the schema.
-- All seeded accounts use the password: Roomix@123
--
-- Seeded users:
--   admin.seed       / admin.seed@roomix.local       / ADMIN
--   landlord.seed    / landlord.seed@roomix.local    / LANDLORD
--   tenant.seed      / tenant.seed@roomix.local      / TENANT

BEGIN;

INSERT INTO users (
    id, username, email, password_hash, full_name, phone_number,
    avatar_url, role, status, created_at, updated_at, password_changed_at
) VALUES
(
    '00000000-0000-0000-0000-000000000001',
    'admin.seed',
    'admin.seed@roomix.local',
    '$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS',
    'Roomix Seed Admin', '+84900000001', NULL, 'ADMIN', 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    '00000000-0000-0000-0000-000000000002',
    'landlord.seed',
    'landlord.seed@roomix.local',
    '$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS',
    'Roomix Seed Landlord', '+84900000002', NULL, 'LANDLORD', 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    '00000000-0000-0000-0000-000000000003',
    'tenant.seed',
    'tenant.seed@roomix.local',
    '$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS',
    'Roomix Seed Tenant', '+84900000003', NULL, 'TENANT', 'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO UPDATE SET
    username = EXCLUDED.username,
    email = EXCLUDED.email,
    password_hash = EXCLUDED.password_hash,
    full_name = EXCLUDED.full_name,
    phone_number = EXCLUDED.phone_number,
    role = EXCLUDED.role,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP,
    password_changed_at = CURRENT_TIMESTAMP;

INSERT INTO houses (
    id, landlord_id, name, address_street, ward, district, city,
    latitude, longitude, description, total_floors, created_at, updated_at
) VALUES (
    '00000000-0000-0000-0000-000000000011',
    '00000000-0000-0000-0000-000000000002',
    'Nhà trọ Seed Roomix', '12 Đường Mẫu', 'Phường Bến Nghé',
    'Quận 1', 'Hồ Chí Minh', 10.7768890, 106.7008060,
    'Dữ liệu mẫu phục vụ kiểm thử E2E Roomix.', 2,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO UPDATE SET
    landlord_id = EXCLUDED.landlord_id,
    name = EXCLUDED.name,
    address_street = EXCLUDED.address_street,
    ward = EXCLUDED.ward,
    district = EXCLUDED.district,
    city = EXCLUDED.city,
    description = EXCLUDED.description,
    total_floors = EXCLUDED.total_floors,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO floors (id, house_id, floor_number, name, description)
VALUES
(
    '00000000-0000-0000-0000-000000000021',
    '00000000-0000-0000-0000-000000000011',
    1, 'Tầng 1', 'Tầng mẫu cho kiểm thử tìm kiếm và yêu cầu thuê.'
),
(
    '00000000-0000-0000-0000-000000000022',
    '00000000-0000-0000-0000-000000000011',
    2, 'Tầng 2', 'Tầng mẫu cho kiểm thử kiểm duyệt bài đăng.'
)
ON CONFLICT (id) DO UPDATE SET
    house_id = EXCLUDED.house_id,
    floor_number = EXCLUDED.floor_number,
    name = EXCLUDED.name,
    description = EXCLUDED.description;

INSERT INTO rooms (
    id, floor_id, room_number, area, base_price, max_tenants, status,
    has_private_bathroom, has_air_conditioner, has_water_heater,
    has_balcony, amenities_description, is_rented, created_at, updated_at
) VALUES
(
    '00000000-0000-0000-0000-000000000031',
    '00000000-0000-0000-0000-000000000021',
    '101', 28.00, 3500000.00, 2, 'AVAILABLE',
    TRUE, TRUE, TRUE, TRUE,
    'Wifi, máy giặt chung, chỗ để xe.', FALSE,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    '00000000-0000-0000-0000-000000000032',
    '00000000-0000-0000-0000-000000000022',
    '201', 32.00, 4200000.00, 2, 'AVAILABLE',
    TRUE, TRUE, TRUE, FALSE,
    'Wifi, chỗ để xe, camera an ninh.', FALSE,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO UPDATE SET
    floor_id = EXCLUDED.floor_id,
    room_number = EXCLUDED.room_number,
    area = EXCLUDED.area,
    base_price = EXCLUDED.base_price,
    max_tenants = EXCLUDED.max_tenants,
    status = EXCLUDED.status,
    has_private_bathroom = EXCLUDED.has_private_bathroom,
    has_air_conditioner = EXCLUDED.has_air_conditioner,
    has_water_heater = EXCLUDED.has_water_heater,
    has_balcony = EXCLUDED.has_balcony,
    amenities_description = EXCLUDED.amenities_description,
    is_rented = EXCLUDED.is_rented,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO posts (
    id, landlord_id, room_id, title, description, rental_price,
    deposit_amount, available_from, status, reject_reason, view_count,
    approved_by, approved_at, created_at, updated_at
) VALUES
(
    '00000000-0000-0000-0000-000000000041',
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000031',
    'Phòng 101 đầy đủ tiện nghi gần trung tâm',
    'Phòng sáng, có ban công, WC riêng và điều hòa. Dữ liệu mẫu để kiểm thử tìm kiếm, yêu thích và gửi yêu cầu thuê.',
    3500000.00, 3500000.00, CURRENT_DATE, 'APPROVED', NULL, 0,
    '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    '00000000-0000-0000-0000-000000000042',
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000032',
    'Phòng 201 chờ Admin kiểm duyệt',
    'Bài đăng mẫu đang chờ duyệt để kiểm thử luồng Admin approve/reject.',
    4200000.00, 4200000.00, CURRENT_DATE, 'PENDING', NULL, 0,
    NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO UPDATE SET
    landlord_id = EXCLUDED.landlord_id,
    room_id = EXCLUDED.room_id,
    title = EXCLUDED.title,
    description = EXCLUDED.description,
    rental_price = EXCLUDED.rental_price,
    deposit_amount = EXCLUDED.deposit_amount,
    available_from = EXCLUDED.available_from,
    status = EXCLUDED.status,
    reject_reason = EXCLUDED.reject_reason,
    approved_by = EXCLUDED.approved_by,
    approved_at = EXCLUDED.approved_at,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO room_images (id, room_id, image_url, is_primary, display_order)
VALUES
(
    '00000000-0000-0000-0000-000000000051',
    '00000000-0000-0000-0000-000000000031',
    '/images/rooms/room-510.jpg', TRUE, 0
),
(
    '00000000-0000-0000-0000-000000000052',
    '00000000-0000-0000-0000-000000000032',
    '/images/rooms/room-405.webp', TRUE, 0
)
ON CONFLICT (id) DO UPDATE SET
    room_id = EXCLUDED.room_id,
    image_url = EXCLUDED.image_url,
    is_primary = EXCLUDED.is_primary,
    display_order = EXCLUDED.display_order;

INSERT INTO notifications (
    id, user_id, title, content, type, reference_id, is_read, created_at
)
VALUES
(
    '00000000-0000-0000-0000-000000000061',
    '00000000-0000-0000-0000-000000000002',
    'Chào mừng chủ trọ seed',
    'Tài khoản mẫu đã sẵn sàng cho kiểm thử Roomix.',
    'SYSTEM', NULL, FALSE, CURRENT_TIMESTAMP
),
(
    '00000000-0000-0000-0000-000000000062',
    '00000000-0000-0000-0000-000000000003',
    'Phòng mẫu đã sẵn sàng',
    'Bạn có thể tìm kiếm và gửi yêu cầu thuê phòng 101.',
    'GENERAL', '00000000-0000-0000-0000-000000000041', FALSE, CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO UPDATE SET
    title = EXCLUDED.title,
    content = EXCLUDED.content,
    type = EXCLUDED.type,
    reference_id = EXCLUDED.reference_id,
    is_read = EXCLUDED.is_read;

COMMIT;
