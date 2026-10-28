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
    'Nhà trọ Seed Roomix', '12 Ngõ 100 Cầu Giấy', 'Phường Dịch Vọng',
    'Quận Cầu Giấy', 'Hà Nội', 21.0333000, 105.7940000,
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
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude,
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

-- ---------------------------------------------------------------------------
-- Extra sample data for RAG / search testing (Hà Nội only, all posts APPROVED)
-- ---------------------------------------------------------------------------
INSERT INTO houses (
    id, landlord_id, name, address_street, ward, district, city,
    latitude, longitude, description, total_floors, created_at, updated_at
) VALUES
(
    '00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000002',
    'Nhà trọ Sinh viên Mỹ Đình', '45 Lê Đức Thọ', 'Phường Mỹ Đình 1',
    'Quận Nam Từ Liêm', 'Hà Nội', 21.0286000, 105.7646000,
    'Khu trọ gần Bến xe Mỹ Đình và các trường đại học, giờ giấc tự do, an ninh tốt.', 2,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    '00000000-0000-0000-0000-000000000013', '00000000-0000-0000-0000-000000000002',
    'Chung cư mini Thanh Xuân', '88 Nguyễn Trãi', 'Phường Thượng Đình',
    'Quận Thanh Xuân', 'Hà Nội', 20.9950000, 105.8100000,
    'Chung cư mini có thang máy, hầm xe, gần ngã tư Sở và trung tâm.', 2,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    '00000000-0000-0000-0000-000000000014', '00000000-0000-0000-0000-000000000002',
    'Nhà trọ Hà Đông', '21 Quang Trung', 'Phường Nguyễn Trãi',
    'Quận Hà Đông', 'Hà Nội', 20.9610000, 105.7690000,
    'Khu dân cư yên tĩnh, gần Aeon Mall Hà Đông và Học viện Bưu chính Viễn thông.', 2,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO UPDATE SET
    landlord_id = EXCLUDED.landlord_id,
    name = EXCLUDED.name,
    address_street = EXCLUDED.address_street,
    ward = EXCLUDED.ward,
    district = EXCLUDED.district,
    city = EXCLUDED.city,
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude,
    description = EXCLUDED.description,
    total_floors = EXCLUDED.total_floors,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO floors (id, house_id, floor_number, name, description)
VALUES
('00000000-0000-0000-0000-000000000023', '00000000-0000-0000-0000-000000000012', 1, 'Tầng 1', 'Phòng giá rẻ cho sinh viên.'),
('00000000-0000-0000-0000-000000000024', '00000000-0000-0000-0000-000000000012', 2, 'Tầng 2', 'Phòng có gác lửng.'),
('00000000-0000-0000-0000-000000000025', '00000000-0000-0000-0000-000000000013', 1, 'Tầng 1', 'Studio đầy đủ nội thất.'),
('00000000-0000-0000-0000-000000000026', '00000000-0000-0000-0000-000000000013', 2, 'Tầng 2', 'Phòng rộng cho cặp đôi.'),
('00000000-0000-0000-0000-000000000027', '00000000-0000-0000-0000-000000000014', 1, 'Tầng 1', 'Phòng thoáng mát, sân để xe riêng.'),
('00000000-0000-0000-0000-000000000028', '00000000-0000-0000-0000-000000000014', 2, 'Tầng 2', 'Phòng cao cấp, ban công.')
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
('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000023', '101', 18.00, 1800000.00, 2, 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 'Wifi miễn phí, nhà vệ sinh chung, chỗ để xe, gần Bến xe Mỹ Đình.', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000102', '00000000-0000-0000-0000-000000000023', '102', 20.00, 2300000.00, 2, 'AVAILABLE', TRUE, FALSE, TRUE, FALSE, 'WC riêng, nóng lạnh, wifi, giờ giấc tự do.', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000103', '00000000-0000-0000-0000-000000000024', '201', 24.00, 2900000.00, 3, 'AVAILABLE', TRUE, TRUE, TRUE, FALSE, 'Có gác lửng, máy lạnh, nóng lạnh, wifi, camera an ninh.', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000104', '00000000-0000-0000-0000-000000000024', '202', 26.00, 3200000.00, 3, 'AVAILABLE', TRUE, TRUE, TRUE, TRUE, 'Gác lửng, ban công, máy lạnh, nóng lạnh, gần chợ.', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000105', '00000000-0000-0000-0000-000000000025', '101', 30.00, 5200000.00, 2, 'AVAILABLE', TRUE, TRUE, TRUE, FALSE, 'Studio full nội thất: giường, tủ, bếp, máy giặt, thang máy, hầm xe.', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000106', '00000000-0000-0000-0000-000000000025', '102', 22.00, 4300000.00, 2, 'AVAILABLE', TRUE, TRUE, TRUE, FALSE, 'Phòng nhỏ gọn, nội thất cơ bản, máy lạnh, thang máy, bảo vệ 24/7.', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000107', '00000000-0000-0000-0000-000000000026', '201', 35.00, 6500000.00, 4, 'AVAILABLE', TRUE, TRUE, TRUE, TRUE, 'Phòng rộng, ban công, bếp riêng, phù hợp cặp đôi hoặc nhóm bạn.', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000108', '00000000-0000-0000-0000-000000000027', '101', 25.00, 4000000.00, 2, 'AVAILABLE', TRUE, TRUE, TRUE, FALSE, 'Sân để xe riêng, máy lạnh, nóng lạnh, khu yên tĩnh, gần Aeon Mall Hà Đông.', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000109', '00000000-0000-0000-0000-000000000028', '201', 32.00, 5800000.00, 3, 'AVAILABLE', TRUE, TRUE, TRUE, TRUE, 'Ban công view thoáng, nội thất cao cấp, máy giặt riêng, wifi cáp quang.', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000110', '00000000-0000-0000-0000-000000000028', '202', 28.00, 4900000.00, 2, 'AVAILABLE', TRUE, TRUE, TRUE, FALSE, 'Phòng mới xây, máy lạnh inverter, khóa vân tay, camera hành lang.', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
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
('00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000101', 'Phòng trọ sinh viên giá rẻ gần Bến xe Mỹ Đình', 'Phòng 18m², giá rẻ nhất khu vực, wifi miễn phí, giờ giấc tự do, phù hợp sinh viên. Điện 3.500đ/kWh, nước 20.000đ/m³.', 1800000.00, 1800000.00, CURRENT_DATE, 'APPROVED', NULL, 0, '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000202', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000102', 'Phòng 20m² có WC riêng và nóng lạnh tại Mỹ Đình', 'Phòng sạch sẽ, WC riêng, nóng lạnh, an ninh tốt, gần chợ và trạm xe buýt. Cọc 1 tháng.', 2300000.00, 2300000.00, CURRENT_DATE, 'APPROVED', NULL, 0, '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000203', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000103', 'Phòng có gác lửng, máy lạnh, camera an ninh Nam Từ Liêm', 'Phòng 24m² có gác lửng rộng, máy lạnh, nóng lạnh. Cho phép nuôi mèo nhỏ. Cọc 1 tháng.', 2900000.00, 2900000.00, CURRENT_DATE, 'APPROVED', NULL, 0, '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000204', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000104', 'Phòng gác lửng + ban công, gần chợ, dưới 3.5 triệu', 'Phòng 26m², gác lửng, ban công thoáng, máy lạnh, nóng lạnh. Phù hợp 2-3 người ở ghép.', 3200000.00, 3200000.00, CURRENT_DATE, 'APPROVED', NULL, 0, '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000205', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000105', 'Studio full nội thất Thanh Xuân có thang máy', 'Studio 30m² full nội thất, bếp, máy giặt, thang máy, hầm xe, bảo vệ 24/7. Gần ngã tư Sở, đi hồ Hoàn Kiếm khoảng 20 phút.', 5200000.00, 5200000.00, CURRENT_DATE, 'APPROVED', NULL, 0, '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000206', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000106', 'Phòng nhỏ gọn nội thất cơ bản Thanh Xuân dưới 4.5 triệu', 'Phòng 22m², máy lạnh, thang máy, bảo vệ 24/7, internet cáp quang. Phù hợp người đi làm độc thân.', 4300000.00, 4300000.00, CURRENT_DATE, 'APPROVED', NULL, 0, '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000207', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000107', 'Phòng rộng 35m² ban công, bếp riêng cho cặp đôi', 'Phòng rộng rãi, ban công, bếp riêng, tối đa 4 người. Gần công viên Hòa Bình.', 6500000.00, 6500000.00, CURRENT_DATE, 'APPROVED', NULL, 0, '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000208', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000108', 'Phòng yên tĩnh có sân để xe riêng gần Aeon Mall Hà Đông', 'Phòng 25m², sân để xe riêng, máy lạnh, nóng lạnh. Khu dân cư văn minh, giờ giấc thoải mái.', 4000000.00, 4000000.00, CURRENT_DATE, 'APPROVED', NULL, 0, '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000209', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000109', 'Phòng cao cấp ban công view thoáng Hà Đông', 'Phòng 32m², nội thất cao cấp, máy giặt riêng, wifi cáp quang, ban công view hồ điều hòa.', 5800000.00, 5800000.00, CURRENT_DATE, 'APPROVED', NULL, 0, '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000210', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000110', 'Phòng mới xây khóa vân tay Hà Đông giá tốt', 'Phòng 28m² mới xây, máy lạnh inverter, khóa vân tay, camera hành lang, gần Học viện Bưu chính Viễn thông.', 4900000.00, 4900000.00, CURRENT_DATE, 'APPROVED', NULL, 0, '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
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
('00000000-0000-0000-0000-000000000301', '00000000-0000-0000-0000-000000000101', '/images/rooms/room-102.webp', TRUE, 0),
('00000000-0000-0000-0000-000000000302', '00000000-0000-0000-0000-000000000102', '/images/rooms/room-201.jpg', TRUE, 0),
('00000000-0000-0000-0000-000000000303', '00000000-0000-0000-0000-000000000103', '/images/rooms/room-302-a.jpg', TRUE, 0),
('00000000-0000-0000-0000-000000000304', '00000000-0000-0000-0000-000000000104', '/images/rooms/room-302-b.jpg', TRUE, 0),
('00000000-0000-0000-0000-000000000305', '00000000-0000-0000-0000-000000000105', '/images/rooms/room-303.jpg', TRUE, 0),
('00000000-0000-0000-0000-000000000306', '00000000-0000-0000-0000-000000000106', '/images/rooms/room-405.webp', TRUE, 0),
('00000000-0000-0000-0000-000000000307', '00000000-0000-0000-0000-000000000107', '/images/rooms/room-510.jpg', TRUE, 0),
('00000000-0000-0000-0000-000000000308', '00000000-0000-0000-0000-000000000108', '/images/rooms/room-201.jpg', TRUE, 0),
('00000000-0000-0000-0000-000000000309', '00000000-0000-0000-0000-000000000109', '/images/rooms/room-302-a.jpg', TRUE, 0),
('00000000-0000-0000-0000-000000000310', '00000000-0000-0000-0000-000000000110', '/images/rooms/room-303.jpg', TRUE, 0)
ON CONFLICT (id) DO UPDATE SET
    room_id = EXCLUDED.room_id,
    image_url = EXCLUDED.image_url,
    is_primary = EXCLUDED.is_primary,
    display_order = EXCLUDED.display_order;

COMMIT;
