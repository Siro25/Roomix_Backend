-- Chuyển ID sang TEXT để lưu UUID và giữ nguyên ID số đã có.
-- Chạy cùng transaction khởi tạo; không thay ID số thành UUID khác.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'face_profiles'
          AND column_name = 'admin_id' AND data_type = 'bigint') THEN
        ALTER TABLE face_profiles DROP CONSTRAINT IF EXISTS face_profiles_admin_id_check;
        ALTER TABLE face_profiles ALTER COLUMN admin_id TYPE TEXT USING admin_id::text;
        ALTER TABLE face_profiles ADD CONSTRAINT face_profiles_admin_id_check
            CHECK (admin_id ~ '^([1-9][0-9]{0,18}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})$');
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'telegram_outbox'
          AND column_name = 'admin_id' AND data_type = 'bigint') THEN
        ALTER TABLE telegram_outbox ALTER COLUMN admin_id TYPE TEXT USING admin_id::text;
    END IF;
END $$;
