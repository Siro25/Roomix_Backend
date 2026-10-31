-- Roomix comprehensive local demo data for rental_db.
-- IDs reference Data.sql in core_db. Cross-database foreign keys are intentionally absent.

BEGIN;

INSERT INTO rental_requests (
    id, room_id, post_id, tenant_id, landlord_id,
    note, status, reject_reason, created_at, updated_at
) VALUES
('00000000-0000-0000-0000-000000006001', '00000000-0000-0000-0000-000000000040', '00000000-0000-0000-0000-000000000050', '00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002', 'Mình muốn thuê lâu dài và có thể nhận phòng sớm.', 'ACCEPTED', NULL, CURRENT_TIMESTAMP - INTERVAL '125 days', CURRENT_TIMESTAMP - INTERVAL '123 days'),
('00000000-0000-0000-0000-000000006002', '00000000-0000-0000-0000-000000000048', '00000000-0000-0000-0000-000000000058', '00000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000002', 'Tôi làm việc gần đây và muốn thuê ít nhất một năm.', 'ACCEPTED', NULL, CURRENT_TIMESTAMP - INTERVAL '65 days', CURRENT_TIMESTAMP - INTERVAL '63 days'),
('00000000-0000-0000-0000-000000006003', '00000000-0000-0000-0000-000000000039', '00000000-0000-0000-0000-000000000049', '00000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000002', 'Mình đã xem phòng và đang chuẩn bị tiền cọc.', 'ACCEPTED', NULL, CURRENT_TIMESTAMP - INTERVAL '8 hours', CURRENT_TIMESTAMP - INTERVAL '7 hours'),
('00000000-0000-0000-0000-000000006004', '00000000-0000-0000-0000-000000000035', '00000000-0000-0000-0000-000000000045', '00000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000002', 'Yêu cầu thuê cũ đã hoàn tất và trả phòng.', 'ACCEPTED', NULL, CURRENT_TIMESTAMP - INTERVAL '330 days', CURRENT_TIMESTAMP - INTERVAL '328 days'),
('00000000-0000-0000-0000-000000006005', '00000000-0000-0000-0000-000000000036', '00000000-0000-0000-0000-000000000046', '00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000002', 'Tôi muốn nuôi thú cưng nhỏ trong phòng.', 'REJECTED', 'Khu nhà hiện không cho phép nuôi thú cưng.', CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP - INTERVAL '5 days'),
('00000000-0000-0000-0000-000000006006', '00000000-0000-0000-0000-000000000037', '00000000-0000-0000-0000-000000000047', '00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000002', 'Mình muốn hẹn xem phòng vào cuối tuần.', 'PENDING', NULL, CURRENT_TIMESTAMP - INTERVAL '3 hours', CURRENT_TIMESTAMP - INTERVAL '3 hours'),
('00000000-0000-0000-0000-000000006007', '00000000-0000-0000-0000-000000000041', '00000000-0000-0000-0000-000000000051', '00000000-0000-0000-0000-000000000013', '00000000-0000-0000-0000-000000000002', 'Tôi cần phòng cho hai người và muốn trao đổi thêm.', 'PENDING', NULL, CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP - INTERVAL '1 hour'),
('00000000-0000-0000-0000-000000006008', '00000000-0000-0000-0000-000000000056', '00000000-0000-0000-0000-000000000066', '00000000-0000-0000-0000-000000000014', '00000000-0000-0000-0000-000000000004', 'Tôi đồng ý các điều khoản và đã chuyển cọc.', 'ACCEPTED', NULL, CURRENT_TIMESTAMP - INTERVAL '95 days', CURRENT_TIMESTAMP - INTERVAL '93 days'),
('00000000-0000-0000-0000-000000006009', '00000000-0000-0000-0000-000000000064', '00000000-0000-0000-0000-000000000074', '00000000-0000-0000-0000-000000000015', '00000000-0000-0000-0000-000000000005', 'Cần phòng gần nơi làm việc, hợp đồng một năm.', 'ACCEPTED', NULL, CURRENT_TIMESTAMP - INTERVAL '45 days', CURRENT_TIMESTAMP - INTERVAL '43 days'),
('00000000-0000-0000-0000-000000006010', '00000000-0000-0000-0000-000000000072', '00000000-0000-0000-0000-000000000082', '00000000-0000-0000-0000-000000000016', '00000000-0000-0000-0000-000000000006', 'Đã xem phòng, mong được nhận phòng trong tháng này.', 'ACCEPTED', NULL, CURRENT_TIMESTAMP - INTERVAL '35 days', CURRENT_TIMESTAMP - INTERVAL '33 days'),
('00000000-0000-0000-0000-000000006011', '00000000-0000-0000-0000-000000000044', '00000000-0000-0000-0000-000000000054', '00000000-0000-0000-0000-000000000017', '00000000-0000-0000-0000-000000000002', 'Tôi đã tìm được phòng khác nên xin hủy yêu cầu.', 'CANCELLED', 'Khách thuê chủ động hủy.', CURRENT_TIMESTAMP - INTERVAL '12 days', CURRENT_TIMESTAMP - INTERVAL '11 days')
ON CONFLICT (id) DO UPDATE SET
    room_id = EXCLUDED.room_id,
    post_id = EXCLUDED.post_id,
    tenant_id = EXCLUDED.tenant_id,
    landlord_id = EXCLUDED.landlord_id,
    note = EXCLUDED.note,
    status = EXCLUDED.status,
    reject_reason = EXCLUDED.reject_reason,
    created_at = EXCLUDED.created_at,
    updated_at = EXCLUDED.updated_at;

INSERT INTO contracts (
    id, contract_code, rental_request_id, room_id, landlord_id, tenant_id,
    start_date, end_date, actual_return_date, monthly_rent, deposit_amount,
    electricity_unit_price, water_unit_price, initial_electricity, initial_water,
    status, terms_and_conditions, cancellation_reason, deposit_confirmed_at,
    created_at, updated_at, deposit_deadline
) VALUES
('00000000-0000-0000-0000-000000006101', 'HD-DEMO-6101', '00000000-0000-0000-0000-000000006001', '00000000-0000-0000-0000-000000000040', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000003', CURRENT_DATE - 120, CURRENT_DATE + 245, NULL, 5650000, 5650000, 5000, 20000, 100, 20, 'ACTIVE', 'Thanh toán tiền phòng trước ngày 05 hằng tháng; giữ gìn tài sản chung.', NULL, CURRENT_TIMESTAMP - INTERVAL '123 days', CURRENT_TIMESTAMP - INTERVAL '124 days', CURRENT_TIMESTAMP - INTERVAL '120 days', CURRENT_TIMESTAMP - INTERVAL '123 days'),
('00000000-0000-0000-0000-000000006102', 'HD-DEMO-6102', '00000000-0000-0000-0000-000000006002', '00000000-0000-0000-0000-000000000048', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000008', CURRENT_DATE - 60, CURRENT_DATE + 305, NULL, 5300000, 5300000, 5000, 20000, 50, 10, 'ACTIVE', 'Hợp đồng 12 tháng, thanh toán bằng chuyển khoản hoặc tiền mặt.', NULL, CURRENT_TIMESTAMP - INTERVAL '63 days', CURRENT_TIMESTAMP - INTERVAL '64 days', CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '63 days'),
('00000000-0000-0000-0000-000000006103', 'HD-DEMO-6103', '00000000-0000-0000-0000-000000006003', '00000000-0000-0000-0000-000000000039', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000009', CURRENT_DATE + 3, CURRENT_DATE + 368, NULL, 4950000, 4950000, 5000, 20000, 0, 0, 'PENDING', 'Hợp đồng có hiệu lực sau khi chủ trọ xác nhận tiền cọc.', NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '7 hours', CURRENT_TIMESTAMP - INTERVAL '7 hours', CURRENT_TIMESTAMP + INTERVAL '17 hours'),
('00000000-0000-0000-0000-000000006104', 'HD-DEMO-6104', '00000000-0000-0000-0000-000000006004', '00000000-0000-0000-0000-000000000035', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000010', CURRENT_DATE - 320, CURRENT_DATE - 20, CURRENT_DATE - 20, 3900000, 3900000, 4500, 18000, 0, 0, 'ENDED', 'Hợp đồng cũ đã hoàn tất quyết toán.', NULL, CURRENT_TIMESTAMP - INTERVAL '328 days', CURRENT_TIMESTAMP - INTERVAL '329 days', CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '328 days'),
('00000000-0000-0000-0000-000000006105', 'HD-DEMO-6105', '00000000-0000-0000-0000-000000006008', '00000000-0000-0000-0000-000000000056', '00000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000014', CURRENT_DATE - 90, CURRENT_DATE + 275, NULL, 3550000, 3550000, 4500, 18000, 20, 5, 'ACTIVE', 'Giữ gìn vệ sinh và tuân thủ quy định an ninh.', NULL, CURRENT_TIMESTAMP - INTERVAL '93 days', CURRENT_TIMESTAMP - INTERVAL '94 days', CURRENT_TIMESTAMP - INTERVAL '90 days', CURRENT_TIMESTAMP - INTERVAL '93 days'),
('00000000-0000-0000-0000-000000006106', 'HD-DEMO-6106', '00000000-0000-0000-0000-000000006009', '00000000-0000-0000-0000-000000000064', '00000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000015', CURRENT_DATE - 40, CURRENT_DATE + 325, NULL, 3200000, 3200000, 4500, 18000, 10, 2, 'ACTIVE', 'Không gây tiếng ồn sau 22 giờ.', NULL, CURRENT_TIMESTAMP - INTERVAL '43 days', CURRENT_TIMESTAMP - INTERVAL '44 days', CURRENT_TIMESTAMP - INTERVAL '40 days', CURRENT_TIMESTAMP - INTERVAL '43 days'),
('00000000-0000-0000-0000-000000006107', 'HD-DEMO-6107', '00000000-0000-0000-0000-000000006010', '00000000-0000-0000-0000-000000000072', '00000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000016', CURRENT_DATE - 30, CURRENT_DATE + 335, NULL, 2850000, 2850000, 4500, 18000, 0, 0, 'ACTIVE', 'Thanh toán đúng hạn, không tự ý sửa chữa phòng.', NULL, CURRENT_TIMESTAMP - INTERVAL '33 days', CURRENT_TIMESTAMP - INTERVAL '34 days', CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '33 days')
ON CONFLICT (id) DO UPDATE SET
    status = EXCLUDED.status,
    actual_return_date = EXCLUDED.actual_return_date,
    deposit_confirmed_at = EXCLUDED.deposit_confirmed_at,
    updated_at = EXCLUDED.updated_at,
    deposit_deadline = EXCLUDED.deposit_deadline;

INSERT INTO room_booking_locks (room_id)
SELECT room_id FROM contracts WHERE id IN (
    '00000000-0000-0000-0000-000000006101', '00000000-0000-0000-0000-000000006102',
    '00000000-0000-0000-0000-000000006103', '00000000-0000-0000-0000-000000006105',
    '00000000-0000-0000-0000-000000006106', '00000000-0000-0000-0000-000000006107'
)
ON CONFLICT (room_id) DO NOTHING;

INSERT INTO active_tenancies (id, room_id, contract_id, tenant_id, check_in_date, assigned_at) VALUES
('00000000-0000-0000-0000-000000006201', '00000000-0000-0000-0000-000000000040', '00000000-0000-0000-0000-000000006101', '00000000-0000-0000-0000-000000000003', CURRENT_DATE - 120, CURRENT_TIMESTAMP - INTERVAL '120 days'),
('00000000-0000-0000-0000-000000006202', '00000000-0000-0000-0000-000000000048', '00000000-0000-0000-0000-000000006102', '00000000-0000-0000-0000-000000000008', CURRENT_DATE - 60, CURRENT_TIMESTAMP - INTERVAL '60 days'),
('00000000-0000-0000-0000-000000006203', '00000000-0000-0000-0000-000000000056', '00000000-0000-0000-0000-000000006105', '00000000-0000-0000-0000-000000000014', CURRENT_DATE - 90, CURRENT_TIMESTAMP - INTERVAL '90 days'),
('00000000-0000-0000-0000-000000006204', '00000000-0000-0000-0000-000000000064', '00000000-0000-0000-0000-000000006106', '00000000-0000-0000-0000-000000000015', CURRENT_DATE - 40, CURRENT_TIMESTAMP - INTERVAL '40 days'),
('00000000-0000-0000-0000-000000006205', '00000000-0000-0000-0000-000000000072', '00000000-0000-0000-0000-000000006107', '00000000-0000-0000-0000-000000000016', CURRENT_DATE - 30, CURRENT_TIMESTAMP - INTERVAL '30 days')
ON CONFLICT (id) DO UPDATE SET
    room_id = EXCLUDED.room_id,
    contract_id = EXCLUDED.contract_id,
    tenant_id = EXCLUDED.tenant_id,
    check_in_date = EXCLUDED.check_in_date,
    assigned_at = EXCLUDED.assigned_at;

-- Ten monthly invoice samples, including paid, pending, confirmed and overdue rows.
WITH invoice_seed(serial, contract_seed, room_seed, landlord_seed, tenant_seed, month_offset, monthly_rent, electricity_price, water_price, initial_electricity, initial_water) AS (
    VALUES
        (1, 6101, 40, 2, 3, 2, 5650000::numeric, 5000::numeric, 20000::numeric, 100::numeric, 20::numeric),
        (2, 6101, 40, 2, 3, 1, 5650000::numeric, 5000::numeric, 20000::numeric, 100::numeric, 20::numeric),
        (3, 6101, 40, 2, 3, 0, 5650000::numeric, 5000::numeric, 20000::numeric, 100::numeric, 20::numeric),
        (4, 6102, 48, 2, 8, 1, 5300000::numeric, 5000::numeric, 20000::numeric, 50::numeric, 10::numeric),
        (5, 6102, 48, 2, 8, 0, 5300000::numeric, 5000::numeric, 20000::numeric, 50::numeric, 10::numeric),
        (6, 6105, 56, 4, 14, 0, 3550000::numeric, 4500::numeric, 18000::numeric, 20::numeric, 5::numeric),
        (7, 6106, 64, 5, 15, 0, 3200000::numeric, 4500::numeric, 18000::numeric, 10::numeric, 2::numeric),
        (8, 6107, 72, 6, 16, 0, 2850000::numeric, 4500::numeric, 18000::numeric, 0::numeric, 0::numeric),
        (9, 6104, 35, 2, 10, 2, 3900000::numeric, 4500::numeric, 18000::numeric, 0::numeric, 0::numeric),
        (10, 6104, 35, 2, 10, 1, 3900000::numeric, 4500::numeric, 18000::numeric, 0::numeric, 0::numeric)
), readings AS (
    SELECT
        seed.*,
        row_number() OVER (PARTITION BY contract_seed ORDER BY month_offset DESC) - 1 AS period_index,
        to_char(date_trunc('month', CURRENT_DATE) - make_interval(months => month_offset), 'YYYY-MM') AS billing_month
    FROM invoice_seed seed
)
INSERT INTO meter_readings (
    id, contract_id, room_id, billing_month,
    old_electricity_val, new_electricity_val, electricity_usage,
    old_water_val, new_water_val, water_usage,
    recorded_by, reading_date, created_at
)
SELECT
    ('00000000-0000-0000-0000-' || lpad((7000 + serial)::text, 12, '0'))::uuid,
    ('00000000-0000-0000-0000-' || lpad(contract_seed::text, 12, '0'))::uuid,
    ('00000000-0000-0000-0000-' || lpad(room_seed::text, 12, '0'))::uuid,
    billing_month,
    initial_electricity + period_index * 30,
    initial_electricity + period_index * 30 + 30,
    30,
    initial_water + period_index * 3,
    initial_water + period_index * 3 + 3,
    3,
    ('00000000-0000-0000-0000-' || lpad(landlord_seed::text, 12, '0'))::uuid,
    LEAST(CURRENT_DATE, (date_trunc('month', CURRENT_DATE) - make_interval(months => month_offset) + INTERVAL '25 days')::date),
    CURRENT_TIMESTAMP - make_interval(days => month_offset * 28)
FROM readings
ON CONFLICT (id) DO UPDATE SET
    billing_month = EXCLUDED.billing_month,
    old_electricity_val = EXCLUDED.old_electricity_val,
    new_electricity_val = EXCLUDED.new_electricity_val,
    electricity_usage = EXCLUDED.electricity_usage,
    old_water_val = EXCLUDED.old_water_val,
    new_water_val = EXCLUDED.new_water_val,
    water_usage = EXCLUDED.water_usage,
    reading_date = EXCLUDED.reading_date;

WITH invoice_seed(serial, contract_seed, month_offset, monthly_rent, electricity_price, water_price) AS (
    VALUES
        (1, 6101, 2, 5650000::numeric, 5000::numeric, 20000::numeric),
        (2, 6101, 1, 5650000::numeric, 5000::numeric, 20000::numeric),
        (3, 6101, 0, 5650000::numeric, 5000::numeric, 20000::numeric),
        (4, 6102, 1, 5300000::numeric, 5000::numeric, 20000::numeric),
        (5, 6102, 0, 5300000::numeric, 5000::numeric, 20000::numeric),
        (6, 6105, 0, 3550000::numeric, 4500::numeric, 18000::numeric),
        (7, 6106, 0, 3200000::numeric, 4500::numeric, 18000::numeric),
        (8, 6107, 0, 2850000::numeric, 4500::numeric, 18000::numeric),
        (9, 6104, 2, 3900000::numeric, 4500::numeric, 18000::numeric),
        (10, 6104, 1, 3900000::numeric, 4500::numeric, 18000::numeric)
), calculated AS (
    SELECT
        *,
        monthly_rent + 30 * electricity_price + 3 * water_price AS total_amount,
        CASE WHEN serial IN (1, 2, 5, 8, 9, 10) THEN 'PAID' WHEN serial IN (4, 7) THEN 'CONFIRMED' ELSE 'PENDING' END AS invoice_status,
        date_trunc('month', CURRENT_DATE) - make_interval(months => month_offset) AS month_start
    FROM invoice_seed
)
INSERT INTO invoices (
    id, invoice_code, contract_id, meter_reading_id, billing_month, due_date,
    total_amount, paid_amount, status, is_disputed, dispute_reason,
    dispute_resolution, confirmed_at, payment_date, payment_method,
    created_at, updated_at, revision
)
SELECT
    ('00000000-0000-0000-0000-' || lpad((7100 + serial)::text, 12, '0'))::uuid,
    'INV-DEMO-' || lpad(serial::text, 3, '0'),
    ('00000000-0000-0000-0000-' || lpad(contract_seed::text, 12, '0'))::uuid,
    ('00000000-0000-0000-0000-' || lpad((7000 + serial)::text, 12, '0'))::uuid,
    to_char(month_start, 'YYYY-MM'),
    (month_start + INTERVAL '10 days')::date,
    total_amount,
    CASE WHEN invoice_status = 'PAID' THEN total_amount ELSE 0 END,
    invoice_status,
    serial = 7,
    CASE WHEN serial = 7 THEN 'Chỉ số điện chưa khớp với đồng hồ thực tế.' ELSE NULL END,
    NULL,
    CASE WHEN invoice_status IN ('CONFIRMED', 'PAID') THEN month_start + INTERVAL '6 days' ELSE NULL END,
    CASE WHEN invoice_status = 'PAID' THEN LEAST(CURRENT_TIMESTAMP, month_start + INTERVAL '8 days') ELSE NULL END,
    CASE WHEN invoice_status = 'PAID' THEN CASE WHEN serial % 2 = 0 THEN 'BANK_TRANSFER' ELSE 'CASH' END ELSE NULL END,
    month_start + INTERVAL '1 day',
    CURRENT_TIMESTAMP,
    0
FROM calculated
ON CONFLICT (id) DO UPDATE SET
    billing_month = EXCLUDED.billing_month,
    due_date = EXCLUDED.due_date,
    total_amount = EXCLUDED.total_amount,
    paid_amount = EXCLUDED.paid_amount,
    status = EXCLUDED.status,
    is_disputed = EXCLUDED.is_disputed,
    dispute_reason = EXCLUDED.dispute_reason,
    confirmed_at = EXCLUDED.confirmed_at,
    payment_date = EXCLUDED.payment_date,
    payment_method = EXCLUDED.payment_method,
    updated_at = EXCLUDED.updated_at;

WITH invoice_seed(serial, monthly_rent, electricity_price, water_price) AS (
    VALUES
        (1, 5650000::numeric, 5000::numeric, 20000::numeric), (2, 5650000::numeric, 5000::numeric, 20000::numeric),
        (3, 5650000::numeric, 5000::numeric, 20000::numeric), (4, 5300000::numeric, 5000::numeric, 20000::numeric),
        (5, 5300000::numeric, 5000::numeric, 20000::numeric), (6, 3550000::numeric, 4500::numeric, 18000::numeric),
        (7, 3200000::numeric, 4500::numeric, 18000::numeric), (8, 2850000::numeric, 4500::numeric, 18000::numeric),
        (9, 3900000::numeric, 4500::numeric, 18000::numeric), (10, 3900000::numeric, 4500::numeric, 18000::numeric)
), item_seed AS (
    SELECT seed.*, item_type
    FROM invoice_seed seed
    CROSS JOIN (VALUES ('ROOM_RENT'), ('ELECTRICITY'), ('WATER')) AS item(item_type)
)
INSERT INTO invoice_items (id, invoice_id, item_name, item_type, quantity, unit_price, total_price)
SELECT
    ('00000000-0000-0000-0000-' || lpad((7200 + serial * 3 + CASE item_type WHEN 'ROOM_RENT' THEN 0 WHEN 'ELECTRICITY' THEN 1 ELSE 2 END)::text, 12, '0'))::uuid,
    ('00000000-0000-0000-0000-' || lpad((7100 + serial)::text, 12, '0'))::uuid,
    CASE item_type WHEN 'ROOM_RENT' THEN 'Tiền phòng' WHEN 'ELECTRICITY' THEN 'Tiền điện' ELSE 'Tiền nước' END,
    item_type,
    CASE item_type WHEN 'ROOM_RENT' THEN 1 ELSE CASE WHEN item_type = 'ELECTRICITY' THEN 30 ELSE 3 END END,
    CASE item_type WHEN 'ROOM_RENT' THEN monthly_rent WHEN 'ELECTRICITY' THEN electricity_price ELSE water_price END,
    CASE item_type WHEN 'ROOM_RENT' THEN monthly_rent WHEN 'ELECTRICITY' THEN 30 * electricity_price ELSE 3 * water_price END
FROM item_seed
ON CONFLICT (id) DO UPDATE SET
    quantity = EXCLUDED.quantity,
    unit_price = EXCLUDED.unit_price,
    total_price = EXCLUDED.total_price;

INSERT INTO invoice_reminders (
    id, invoice_id, tenant_id, landlord_id, reminder_date,
    outstanding_amount, content, created_at
) VALUES
('00000000-0000-0000-0000-000000007301', '00000000-0000-0000-0000-000000007104', '00000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000002', CURRENT_DATE - 5, 5510000, 'Hóa đơn kỳ trước đã quá hạn, vui lòng kiểm tra và thanh toán.', CURRENT_TIMESTAMP - INTERVAL '5 days'),
('00000000-0000-0000-0000-000000007302', '00000000-0000-0000-0000-000000007103', '00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002', CURRENT_DATE, 5860000, 'Hóa đơn mới đã được phát hành, vui lòng xác nhận trước hạn.', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET
    reminder_date = EXCLUDED.reminder_date,
    outstanding_amount = EXCLUDED.outstanding_amount,
    content = EXCLUDED.content,
    created_at = EXCLUDED.created_at;

INSERT INTO contact_cards (
    id, rental_request_id, landlord_id, landlord_name, landlord_phone,
    landlord_zalo, landlord_avatar_url, note, issued_at
) VALUES
('00000000-0000-0000-0000-000000007401', '00000000-0000-0000-0000-000000006001', '00000000-0000-0000-0000-000000000002', 'Nguyễn Minh Hoàng', '+84900000002', '0900000002', NULL, 'Liên hệ Zalo để thống nhất thời gian nhận phòng.', CURRENT_TIMESTAMP - INTERVAL '123 days'),
('00000000-0000-0000-0000-000000007402', '00000000-0000-0000-0000-000000006002', '00000000-0000-0000-0000-000000000002', 'Nguyễn Minh Hoàng', '+84900000002', '0900000002', NULL, 'Chủ trọ sẽ gửi hợp đồng và hướng dẫn nhận phòng qua Zalo.', CURRENT_TIMESTAMP - INTERVAL '63 days'),
('00000000-0000-0000-0000-000000007403', '00000000-0000-0000-0000-000000006003', '00000000-0000-0000-0000-000000000002', 'Nguyễn Minh Hoàng', '+84900000002', '0900000002', NULL, 'Vui lòng hoàn tất cọc trong thời hạn giữ chỗ.', CURRENT_TIMESTAMP - INTERVAL '7 hours')
ON CONFLICT (id) DO UPDATE SET
    note = EXCLUDED.note,
    issued_at = EXCLUDED.issued_at;

INSERT INTO tenancy_events (
    event_id, event_type, room_id, tenancy_id, partition_key,
    topic, occurred_at, published_at, attempt_count, next_attempt_at, last_error
) VALUES
('00000000-0000-0000-0000-000000007501', 'TENANCY_ACTIVATED', '00000000-0000-0000-0000-000000000040', '00000000-0000-0000-0000-000000006101', '00000000-0000-0000-0000-000000000040', 'rental-events-topic', CURRENT_TIMESTAMP - INTERVAL '120 days', CURRENT_TIMESTAMP - INTERVAL '120 days', 1, CURRENT_TIMESTAMP - INTERVAL '120 days', NULL),
('00000000-0000-0000-0000-000000007502', 'TENANCY_ACTIVATED', '00000000-0000-0000-0000-000000000048', '00000000-0000-0000-0000-000000006102', '00000000-0000-0000-0000-000000000048', 'rental-events-topic', CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '60 days', 1, CURRENT_TIMESTAMP - INTERVAL '60 days', NULL),
('00000000-0000-0000-0000-000000007503', 'TENANCY_ENDED', '00000000-0000-0000-0000-000000000035', '00000000-0000-0000-0000-000000006104', '00000000-0000-0000-0000-000000000035', 'rental-events-topic', CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '20 days', 1, CURRENT_TIMESTAMP - INTERVAL '20 days', NULL)
ON CONFLICT (event_id) DO UPDATE SET
    event_type = EXCLUDED.event_type,
    room_id = EXCLUDED.room_id,
    tenancy_id = EXCLUDED.tenancy_id,
    partition_key = EXCLUDED.partition_key,
    topic = EXCLUDED.topic,
    occurred_at = EXCLUDED.occurred_at,
    published_at = EXCLUDED.published_at,
    attempt_count = EXCLUDED.attempt_count,
    next_attempt_at = EXCLUDED.next_attempt_at,
    last_error = EXCLUDED.last_error;

COMMIT;
