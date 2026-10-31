-- Roomix comprehensive local demo data for core_db.
-- Run after Flyway migrations. All demo accounts use: Roomix@123
-- Main accounts: admin.seed, landlord.seed, tenant.seed
-- Additional accounts: landlord.demo.4..7 and tenant.demo.8..19

BEGIN;

-- -----------------------------------------------------------------------------
-- Users
-- -----------------------------------------------------------------------------
INSERT INTO users (
    id, username, email, password_hash, full_name, phone_number, avatar_url,
    role, status, created_at, updated_at, password_changed_at, zalo
) VALUES
(
    '00000000-0000-0000-0000-000000000001', 'admin.seed',
    'admin.seed@roomix.local', '$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS',
    'Quản trị viên Roomix', '+84900000001', NULL, 'ADMIN', 'ACTIVE',
    CURRENT_TIMESTAMP - INTERVAL '365 days', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL
),
(
    '00000000-0000-0000-0000-000000000002', 'landlord.seed',
    'landlord.seed@roomix.local', '$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS',
    'Nguyễn Minh Hoàng', '+84900000002', NULL, 'LANDLORD', 'ACTIVE',
    CURRENT_TIMESTAMP - INTERVAL '300 days', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '0900000002'
),
(
    '00000000-0000-0000-0000-000000000003', 'tenant.seed',
    'tenant.seed@roomix.local', '$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS',
    'Trần Gia Hân', '+84900000003', NULL, 'TENANT', 'ACTIVE',
    CURRENT_TIMESTAMP - INTERVAL '180 days', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL
)
ON CONFLICT (id) DO UPDATE SET
    username = EXCLUDED.username,
    email = EXCLUDED.email,
    password_hash = EXCLUDED.password_hash,
    full_name = EXCLUDED.full_name,
    phone_number = EXCLUDED.phone_number,
    avatar_url = EXCLUDED.avatar_url,
    role = EXCLUDED.role,
    status = EXCLUDED.status,
    zalo = EXCLUDED.zalo,
    updated_at = CURRENT_TIMESTAMP,
    password_changed_at = CURRENT_TIMESTAMP;

WITH demo_users(seed, role, status, full_name) AS (
    VALUES
        (4, 'LANDLORD', 'ACTIVE', 'Lê Quốc Bảo'),
        (5, 'LANDLORD', 'ACTIVE', 'Phạm Thùy Dương'),
        (6, 'LANDLORD', 'ACTIVE', 'Võ Đức Anh'),
        (7, 'LANDLORD', 'PENDING_VERIFICATION', 'Đặng Khánh Linh'),
        (8, 'TENANT', 'ACTIVE', 'Nguyễn Tuấn Kiệt'),
        (9, 'TENANT', 'ACTIVE', 'Trương Mỹ Duyên'),
        (10, 'TENANT', 'ACTIVE', 'Phan Hoàng Nam'),
        (11, 'TENANT', 'ACTIVE', 'Bùi Thanh Trúc'),
        (12, 'TENANT', 'ACTIVE', 'Lý Minh Khang'),
        (13, 'TENANT', 'ACTIVE', 'Đỗ Ngọc Mai'),
        (14, 'TENANT', 'ACTIVE', 'Hồ Anh Quân'),
        (15, 'TENANT', 'ACTIVE', 'Dương Bảo Châu'),
        (16, 'TENANT', 'ACTIVE', 'Mai Nhật Long'),
        (17, 'TENANT', 'ACTIVE', 'Tạ Thu Hà'),
        (18, 'TENANT', 'ACTIVE', 'Vũ Quốc Huy'),
        (19, 'TENANT', 'LOCKED', 'Ngô Phương Thảo')
)
INSERT INTO users (
    id, username, email, password_hash, full_name, phone_number, avatar_url,
    role, status, created_at, updated_at, password_changed_at, zalo
)
SELECT
    ('00000000-0000-0000-0000-' || lpad(seed::text, 12, '0'))::uuid,
    CASE WHEN role = 'LANDLORD' THEN 'landlord.demo.' ELSE 'tenant.demo.' END || seed,
    CASE WHEN role = 'LANDLORD' THEN 'landlord.demo.' ELSE 'tenant.demo.' END || seed || '@roomix.local',
    '$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS',
    full_name,
    '+8491' || lpad(seed::text, 8, '0'),
    NULL,
    role,
    status,
    CURRENT_TIMESTAMP - make_interval(days => 200 - seed * 3),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CASE WHEN role = 'LANDLORD' THEN '091' || lpad(seed::text, 7, '0') ELSE NULL END
FROM demo_users
ON CONFLICT (id) DO UPDATE SET
    username = EXCLUDED.username,
    email = EXCLUDED.email,
    password_hash = EXCLUDED.password_hash,
    full_name = EXCLUDED.full_name,
    phone_number = EXCLUDED.phone_number,
    role = EXCLUDED.role,
    status = EXCLUDED.status,
    zalo = EXCLUDED.zalo,
    updated_at = CURRENT_TIMESTAMP,
    password_changed_at = CURRENT_TIMESTAMP;

-- -----------------------------------------------------------------------------
-- Houses and floors
-- -----------------------------------------------------------------------------
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

WITH floor_seed AS (
    SELECT
        seed,
        11 + ((seed - 21) / 2) AS house_seed,
        1 + ((seed - 21) % 2) AS floor_number
    FROM generate_series(21, 34) AS seed
)
INSERT INTO floors (id, house_id, floor_number, name, description)
SELECT
    ('00000000-0000-0000-0000-' || lpad(seed::text, 12, '0'))::uuid,
    ('00000000-0000-0000-0000-' || lpad(house_seed::text, 12, '0'))::uuid,
    floor_number,
    'Tầng ' || floor_number,
    'Tầng ' || floor_number || ' gồm ba phòng demo với nhiều trạng thái khác nhau.'
FROM floor_seed
ON CONFLICT (id) DO UPDATE SET
    house_id = EXCLUDED.house_id,
    floor_number = EXCLUDED.floor_number,
    name = EXCLUDED.name,
    description = EXCLUDED.description;

-- -----------------------------------------------------------------------------
-- Rooms, posts and images: 42 rooms, seven houses, five post states.
-- -----------------------------------------------------------------------------
WITH room_seed AS (
    SELECT
        seed,
        21 + ((seed - 31) / 3) AS floor_seed,
        1 + ((seed - 31) % 3) AS room_index,
        CASE
            WHEN seed IN (40, 48, 56, 64, 72) THEN 'OCCUPIED'
            WHEN seed IN (39, 55, 71) THEN 'RESERVED'
            WHEN seed IN (38, 54, 70) THEN 'MAINTENANCE'
            ELSE 'AVAILABLE'
        END AS room_status
    FROM generate_series(31, 72) AS seed
)
INSERT INTO rooms (
    id, floor_id, room_number, area, base_price, max_tenants, status,
    has_private_bathroom, has_air_conditioner, has_water_heater,
    has_balcony, amenities_description, is_rented, created_at, updated_at
)
SELECT
    ('00000000-0000-0000-0000-' || lpad(seed::text, 12, '0'))::uuid,
    ('00000000-0000-0000-0000-' || lpad(floor_seed::text, 12, '0'))::uuid,
    (((1 + ((floor_seed - 21) % 2)) * 100) + room_index)::text,
    (18 + ((seed - 31) % 8) * 2.5)::numeric(10, 2),
    (2500000 + ((seed - 31) % 9) * 350000)::numeric(15, 2),
    1 + ((seed - 31) % 3),
    room_status,
    TRUE,
    seed % 3 <> 0,
    seed % 4 <> 0,
    seed % 2 = 0,
    CASE seed % 4
        WHEN 0 THEN 'Wifi tốc độ cao, máy giặt riêng, ban công, chỗ để xe.'
        WHEN 1 THEN 'Wifi, máy giặt chung, camera an ninh, giờ giấc tự do.'
        WHEN 2 THEN 'Nội thất cơ bản, tủ lạnh, bếp, khóa vân tay.'
        ELSE 'Gác lửng, cửa sổ lớn, khu phơi đồ và chỗ để xe.'
    END,
    room_status = 'OCCUPIED',
    CURRENT_TIMESTAMP - make_interval(days => 100 - (seed - 31)),
    CURRENT_TIMESTAMP
FROM room_seed
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

WITH post_seed AS (
    SELECT
        room_seed,
        room_seed + 10 AS post_seed,
        CASE
            WHEN room_seed = 31 THEN 'APPROVED'
            WHEN room_seed = 32 THEN 'PENDING'
            WHEN room_seed % 13 = 0 THEN 'DRAFT'
            WHEN room_seed % 11 = 0 THEN 'PENDING'
            WHEN room_seed % 7 = 0 THEN 'REJECTED'
            WHEN room_seed % 5 = 0 THEN 'HIDDEN'
            ELSE 'APPROVED'
        END AS post_status
    FROM generate_series(31, 72) AS room_seed
), details AS (
    SELECT
        seed.*,
        room.room_number,
        room.base_price,
        house.landlord_id,
        house.name AS house_name,
        house.district
    FROM post_seed seed
    JOIN rooms room
      ON room.id = ('00000000-0000-0000-0000-' || lpad(seed.room_seed::text, 12, '0'))::uuid
    JOIN floors floor ON floor.id = room.floor_id
    JOIN houses house ON house.id = floor.house_id
)
INSERT INTO posts (
    id, landlord_id, room_id, title, description, rental_price,
    deposit_amount, available_from, status, reject_reason, view_count,
    approved_by, approved_at, created_at, updated_at
)
SELECT
    ('00000000-0000-0000-0000-' || lpad(post_seed::text, 12, '0'))::uuid,
    landlord_id,
    ('00000000-0000-0000-0000-' || lpad(room_seed::text, 12, '0'))::uuid,
    'Phòng ' || room_number || ' tại ' || house_name,
    'Phòng demo tại ' || district || ', đầy đủ thông tin giá, diện tích và tiện ích để kiểm thử tìm kiếm, lọc, yêu thích và thuê phòng.',
    base_price,
    base_price,
    CURRENT_DATE + ((room_seed - 31) % 10),
    post_status,
    CASE WHEN post_status = 'REJECTED' THEN 'Hình ảnh hoặc mô tả chưa đủ rõ ràng, vui lòng cập nhật lại.' ELSE NULL END,
    35 + (room_seed - 31) * 17,
    CASE WHEN post_status IN ('APPROVED', 'HIDDEN') THEN '00000000-0000-0000-0000-000000000001'::uuid ELSE NULL END,
    CASE WHEN post_status IN ('APPROVED', 'HIDDEN') THEN CURRENT_TIMESTAMP - make_interval(days => (room_seed % 20) + 1) ELSE NULL END,
    CURRENT_TIMESTAMP - make_interval(days => 90 - (room_seed - 31)),
    CURRENT_TIMESTAMP
FROM details
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
    view_count = EXCLUDED.view_count,
    approved_by = EXCLUDED.approved_by,
    approved_at = EXCLUDED.approved_at,
    updated_at = CURRENT_TIMESTAMP;

WITH image_seed AS (
    SELECT seed, seed + 100 AS image_seed
    FROM generate_series(31, 72) AS seed
)
INSERT INTO room_images (id, room_id, image_url, is_primary, display_order)
SELECT
    ('00000000-0000-0000-0000-' || lpad(image_seed::text, 12, '0'))::uuid,
    ('00000000-0000-0000-0000-' || lpad(seed::text, 12, '0'))::uuid,
    CASE seed % 7
        WHEN 0 THEN '/images/rooms/room-510.jpg'
        WHEN 1 THEN '/images/rooms/room-405.webp'
        WHEN 2 THEN '/images/rooms/room-303.jpg'
        WHEN 3 THEN '/images/rooms/room-302-b.jpg'
        WHEN 4 THEN '/images/rooms/room-302-a.jpg'
        WHEN 5 THEN '/images/rooms/room-201.jpg'
        ELSE '/images/rooms/room-102.webp'
    END,
    TRUE,
    0
FROM image_seed
ON CONFLICT (id) DO UPDATE SET
    room_id = EXCLUDED.room_id,
    image_url = EXCLUDED.image_url,
    is_primary = EXCLUDED.is_primary,
    display_order = EXCLUDED.display_order;

WITH image_seed AS (
    SELECT seed, seed + 200 AS image_seed
    FROM generate_series(31, 72) AS seed
    WHERE seed % 2 = 0
)
INSERT INTO room_images (id, room_id, image_url, is_primary, display_order)
SELECT
    ('00000000-0000-0000-0000-' || lpad(image_seed::text, 12, '0'))::uuid,
    ('00000000-0000-0000-0000-' || lpad(seed::text, 12, '0'))::uuid,
    CASE seed % 3
        WHEN 0 THEN '/images/rooms/room-302-a.jpg'
        WHEN 1 THEN '/images/rooms/room-201.jpg'
        ELSE '/images/rooms/room-510.jpg'
    END,
    FALSE,
    1
FROM image_seed
ON CONFLICT (id) DO UPDATE SET
    room_id = EXCLUDED.room_id,
    image_url = EXCLUDED.image_url,
    is_primary = EXCLUDED.is_primary,
    display_order = EXCLUDED.display_order;

-- -----------------------------------------------------------------------------
-- Favorites, violation reports, notifications and audit history.
-- -----------------------------------------------------------------------------
WITH tenant_seed AS (
    SELECT seed FROM generate_series(8, 19) AS seed
    UNION ALL SELECT 3
), approved_posts AS (
    SELECT
        room_seed,
        post.id AS post_id
    FROM generate_series(31, 72) AS room_seed
    JOIN rooms room
      ON room.id = ('00000000-0000-0000-0000-' || lpad(room_seed::text, 12, '0'))::uuid
    JOIN posts post ON post.room_id = room.id
    WHERE post.status IN ('APPROVED', 'HIDDEN')
), pairs AS (
    SELECT
        tenant.seed AS tenant_seed,
        post.room_seed,
        post.post_id,
        row_number() OVER (ORDER BY tenant.seed, post.room_seed) AS row_number
    FROM tenant_seed tenant
    CROSS JOIN approved_posts post
    WHERE (tenant.seed + post.room_seed) % 7 = 0
    ORDER BY tenant.seed, post.room_seed
    LIMIT 60
)
INSERT INTO favorites (id, tenant_id, post_id, created_at)
SELECT
    ('00000000-0000-0000-0000-' || lpad((1000 + row_number)::text, 12, '0'))::uuid,
    ('00000000-0000-0000-0000-' || lpad(tenant_seed::text, 12, '0'))::uuid,
    post_id,
    CURRENT_TIMESTAMP - make_interval(days => room_seed % 25)
FROM pairs
ON CONFLICT (tenant_id, post_id) DO UPDATE SET
    created_at = EXCLUDED.created_at;

INSERT INTO violation_reports (
    id, reporter_id, target_type, target_id, reason, description,
    status, resolution, reviewed_by, reviewed_at, created_at, updated_at
) VALUES
('00000000-0000-0000-0000-000000003001', '00000000-0000-0000-0000-000000000003',
 'ROOM', '00000000-0000-0000-0000-000000000034', 'INACCURATE_INFO',
 'Diện tích thực tế có vẻ nhỏ hơn thông tin bài đăng.', 'PENDING', NULL, NULL, NULL,
 CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours'),
('00000000-0000-0000-0000-000000003002', '00000000-0000-0000-0000-000000000008',
 'ROOM', '00000000-0000-0000-0000-000000000036', 'PRICE_DISCREPANCY',
 'Giá trao đổi qua điện thoại cao hơn giá niêm yết.', 'REVIEWING',
 'Admin đã tiếp nhận và đang liên hệ xác minh.', '00000000-0000-0000-0000-000000000001',
 CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '1 day'),
('00000000-0000-0000-0000-000000003003', '00000000-0000-0000-0000-000000000009',
 'ROOM', '00000000-0000-0000-0000-000000000043', 'DUPLICATE_OR_EXPIRED',
 'Phòng đã có người thuê nhưng bài đăng vẫn hiển thị.', 'RESOLVED',
 'Đã xác minh và yêu cầu chủ trọ cập nhật trạng thái.', '00000000-0000-0000-0000-000000000001',
 CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP - INTERVAL '4 days'),
('00000000-0000-0000-0000-000000003004', '00000000-0000-0000-0000-000000000010',
 'USER', '00000000-0000-0000-0000-000000000006', 'OTHER',
 'Phản hồi tin nhắn chậm và cung cấp thông tin chưa đầy đủ.', 'REJECTED',
 'Không phát hiện hành vi vi phạm quy định hệ thống.', '00000000-0000-0000-0000-000000000001',
 CURRENT_TIMESTAMP - INTERVAL '7 days', CURRENT_TIMESTAMP - INTERVAL '9 days', CURRENT_TIMESTAMP - INTERVAL '7 days'),
('00000000-0000-0000-0000-000000003005', '00000000-0000-0000-0000-000000000011',
 'ROOM', '00000000-0000-0000-0000-000000000052', 'FAKE_DEPOSIT',
 'Người đăng yêu cầu chuyển cọc trước khi xem phòng.', 'PENDING', NULL, NULL, NULL,
 CURRENT_TIMESTAMP - INTERVAL '30 minutes', CURRENT_TIMESTAMP - INTERVAL '30 minutes'),
('00000000-0000-0000-0000-000000003006', '00000000-0000-0000-0000-000000000012',
 'POST', '00000000-0000-0000-0000-000000000099', 'INACCURATE_INFO',
 'Hình ảnh trong bài đăng chưa phản ánh đúng nội thất hiện tại.', 'PENDING', NULL, NULL, NULL,
 CURRENT_TIMESTAMP - INTERVAL '5 hours', CURRENT_TIMESTAMP - INTERVAL '5 hours')
ON CONFLICT (id) DO UPDATE SET
    reporter_id = EXCLUDED.reporter_id,
    target_type = EXCLUDED.target_type,
    target_id = EXCLUDED.target_id,
    reason = EXCLUDED.reason,
    description = EXCLUDED.description,
    status = EXCLUDED.status,
    resolution = EXCLUDED.resolution,
    reviewed_by = EXCLUDED.reviewed_by,
    reviewed_at = EXCLUDED.reviewed_at,
    updated_at = EXCLUDED.updated_at;

WITH notification_seed AS (
    SELECT
        user_seed,
        occurrence,
        row_number() OVER (ORDER BY user_seed, occurrence) AS row_number
    FROM generate_series(1, 19) AS user_seed
    CROSS JOIN generate_series(1, 2) AS occurrence
)
INSERT INTO notifications (
    id, user_id, title, content, type, reference_id, is_read, read_at, created_at
)
SELECT
    ('00000000-0000-0000-0000-' || lpad((4000 + row_number)::text, 12, '0'))::uuid,
    ('00000000-0000-0000-0000-' || lpad(user_seed::text, 12, '0'))::uuid,
    CASE occurrence
        WHEN 1 THEN 'Chào mừng đến với Roomix'
        ELSE CASE WHEN user_seed BETWEEN 2 AND 7 THEN 'Cập nhật hoạt động cho thuê' ELSE 'Gợi ý phòng phù hợp' END
    END,
    CASE occurrence
        WHEN 1 THEN 'Hồ sơ demo đã sẵn sàng để trải nghiệm đầy đủ các chức năng của hệ thống.'
        ELSE CASE WHEN user_seed BETWEEN 2 AND 7
            THEN 'Bạn có dữ liệu phòng, yêu cầu thuê và thống kê mới cần theo dõi.'
            ELSE 'Roomix vừa cập nhật thêm nhiều phòng đã được duyệt tại Thành phố Hồ Chí Minh.' END
    END,
    CASE WHEN occurrence = 1 THEN 'SYSTEM' WHEN user_seed BETWEEN 2 AND 7 THEN 'INTEREST_REQUEST' ELSE 'GENERAL' END,
    CASE WHEN occurrence = 2 THEN '00000000-0000-0000-0000-000000000041'::uuid ELSE NULL END,
    occurrence = 1,
    CASE WHEN occurrence = 1 THEN CURRENT_TIMESTAMP - make_interval(days => user_seed % 10) ELSE NULL END,
    CURRENT_TIMESTAMP - make_interval(hours => user_seed * occurrence * 3)
FROM notification_seed
ON CONFLICT (id) DO UPDATE SET
    user_id = EXCLUDED.user_id,
    title = EXCLUDED.title,
    content = EXCLUDED.content,
    type = EXCLUDED.type,
    reference_id = EXCLUDED.reference_id,
    is_read = EXCLUDED.is_read,
    read_at = EXCLUDED.read_at,
    created_at = EXCLUDED.created_at;

WITH audit_seed AS (
    SELECT seed FROM generate_series(1, 18) AS seed
)
INSERT INTO audit_logs (
    id, user_id, action, entity_name, entity_id,
    old_values, new_values, ip_address, user_agent, created_at
)
SELECT
    ('00000000-0000-0000-0000-' || lpad((5000 + seed)::text, 12, '0'))::uuid,
    '00000000-0000-0000-0000-000000000001'::uuid,
    CASE seed % 4
        WHEN 0 THEN 'APPROVE_POST'
        WHEN 1 THEN 'REVIEW_REPORT'
        WHEN 2 THEN 'APPROVE_LANDLORD'
        ELSE 'LOCK_USER'
    END,
    CASE seed % 4 WHEN 0 THEN 'POST' WHEN 1 THEN 'VIOLATION_REPORT' ELSE 'USER' END,
    ('demo-' || seed),
    jsonb_build_object('status', 'PENDING'),
    jsonb_build_object('status', CASE seed % 4 WHEN 3 THEN 'LOCKED' ELSE 'ACTIVE' END),
    '127.0.0.1',
    'Roomix demo seed',
    CURRENT_TIMESTAMP - make_interval(days => seed)
FROM audit_seed
ON CONFLICT (id) DO UPDATE SET
    action = EXCLUDED.action,
    entity_name = EXCLUDED.entity_name,
    entity_id = EXCLUDED.entity_id,
    old_values = EXCLUDED.old_values,
    new_values = EXCLUDED.new_values,
    created_at = EXCLUDED.created_at;

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
