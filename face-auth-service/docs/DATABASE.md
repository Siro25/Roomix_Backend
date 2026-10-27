# PostgreSQL cho Face Auth

Face Python dùng database `roomix_face` riêng. Core Java có database riêng,
không ghi trực tiếp vào bảng của Face. Phiên đăng nhập chính vẫn thuộc Core;
`face_challenges` chỉ chứa tiến độ xử lý khuôn mặt của phiên đó.

## 1. Khởi tạo database

Kết nối PostgreSQL bằng tài khoản quản trị qua pgAdmin hoặc psql. Chạy từng
lệnh sau, thay mật khẩu bằng giá trị riêng của bạn:

```sql
CREATE ROLE face_user LOGIN PASSWORD 'THAY_BANG_MAT_KHAU_RIENG';
CREATE DATABASE roomix_face OWNER face_user;
```

Không chạy CREATE DATABASE trong transaction. Ứng dụng không tạo role hoặc
database thay bạn; chỉ tạo bảng/index nếu chưa có. Tài khoản Face phải được
phép tạo bảng trong schema `public` của database Face. Không cần superuser.

## 2. Cấu hình và chạy

```powershell
.\scripts\setup_runtime.ps1
$env:FACE_DATABASE_URL = 'postgresql://face_user:MAT_KHAU@localhost:5432/roomix_face'
$env:CORE_URL = 'http://localhost:8081'
$env:FACE_SERVICE_TOKEN = 'BI_MAT_NOI_BO_CUNG_CORE'
.\.venv-runtime\Scripts\python.exe -m uvicorn src.face_auth.main:app --port 8003
```

Nếu mật khẩu chứa ký tự đặc biệt như `@`, `:`, `/`, phải percent-encode thành
phần mật khẩu hợp lệ trong URL. Không commit URL chứa mật khẩu vào Git.
`.env.example` chỉ là mẫu; ứng dụng đọc trực tiếp biến môi trường.
Nếu muốn lưu cấu hình cục bộ trong `.env`, dùng `scripts/run_local.ps1`
theo `RUNTIME.md`. File `.env` đã được loại khỏi Git.
Không còn `FACE_DATABASE_PATH` hoặc chế độ dự phòng SQLite.

## 3. Các bảng

Schema nguồn: `src/face_auth/db/schema.sql`.

| Bảng | Nội dung |
|---|---|
| `face_profiles` | `admin_id` TEXT (UUID hoặc ID số cũ), vector trung bình JSONB, thời gian cập nhật TIMESTAMPTZ |
| `telegram_outbox` | Sự kiện ảnh, retry và thời hạn lưu tạm; chỉ thuộc Face |
| `face_challenges` | Mã thử thách, `auth_session_id` UNIQUE, trạng thái JSONB, thời gian tạo |

`admin_id` do Core cung cấp, không có khóa ngoại sang database Core.
Ảnh tạm được lưu dạng base64 trong trạng thái chỉ khi đang chờ kết quả Core;
hoàn tất sẽ xóa ảnh và các vector tạm. Vector hồ sơ chính vẫn được giữ.
Không cần pgvector: MVP nhận dạng bằng Python sau khi đọc hồ sơ Admin.

`SELECT FOR UPDATE` khóa đúng thử thách đang xử lý. Khi đăng ký, advisory lock
`720031` tuần tự hóa các lượt ENROLL để kiểm tra trùng mặt trước khi lưu.
Lưu hồ sơ và kết thúc thử thách cùng một transaction. Nếu lưu lỗi, cả hai
được rollback. Core callback vẫn cần idempotent như hợp đồng tích hợp.

Startup chạy migration `src/face_auth/db/migrations/001_admin_id_text.sql`
trước `CREATE TABLE IF NOT EXISTS`. Migration chuyển `admin_id` từ BIGINT
sang TEXT trong `face_profiles` và `telegram_outbox`, giữ nguyên giá trị ID,
vector, ảnh đang chờ gửi và trạng thái challenge. Chạy lại không đổi dữ liệu.
ID số vẫn gửi về Core dưới dạng số; UUID gửi nguyên chuỗi đã xác thực.
Không tự ánh xạ tài khoản số cũ sang tài khoản UUID khác.

Migration và tạo bảng chạy trong một transaction, có khóa chống nâng schema
đồng thời. Tài khoản kết nối cần là chủ bảng hoặc có quyền ALTER tương ứng.
Trước khi cập nhật database có dữ liệu cần giữ, sao lưu và dừng tiến trình
Face phiên bản cũ. Không chạy bản cũ song song với schema mới. Migration này
đã có kiểm thử trên database riêng; không tự chạy trên database người dùng
trong phiên phát triển. Lần khởi động service tiếp theo sẽ áp dụng migration.

## 4. Dọn phiên hết hạn

```powershell
.\.venv-runtime\Scripts\python.exe -m src.face_auth.db.cleanup
```

Lệnh xóa nội dung sinh trắc tạm của phiên và ảnh hàng đợi Telegram hết hạn, giữ mã phiên để chống
dùng lại; không xóa hồ sơ Admin. Có thể chạy định kỳ ở môi trường triển khai.

## 5. Kiểm thử database thật

Tạo một database riêng, ví dụ `roomix_face_test`, và tài khoản test có quyền
CREATE SCHEMA trong database đó. Không trỏ URL test vào database sản phẩm.

```powershell
$env:TEST_DATABASE_URL = 'postgresql://face_test:MAT_KHAU@localhost:5432/roomix_face_test'
.\.venv-runtime\Scripts\python.exe -m pip install -r requirements-test.txt
.\.venv-runtime\Scripts\python.exe -m unittest discover -s tests -v
```

Mỗi test tạo schema ngẫu nhiên `face_test_<uuid>` và xóa chính schema đó khi
xong. Kiểm thử dùng PostgreSQL thật, không thay bằng SQLite. Nếu không cấu
hình URL test, các test cần PostgreSQL sẽ SKIP, không phải đã kiểm tra đạt.

Không tự chuyển database SQLite/pickle cũ: danh tính MSSV cũ không bảo đảm
khớp `adminId` của Core. Admin đăng nhập mật khẩu và đăng ký lại mặt qua
luồng có xác thực. Dữ liệu cũ được giữ nguyên để đối chiếu.
