> Sau khi tích hợp main: xem [MAIN_INTEGRATION.md](MAIN_INTEGRATION.md).
> Các lệnh bootstrap Admin, CORE_DB_INIT_MODE và cấu hình JWT cũ bên dưới không còn áp dụng.

# Chạy Core trên PostgreSQL Windows

Đã xác nhận PostgreSQL localhost:5432 đang chạy, database Face là roomix_face.
core_db chưa có; tài khoản Face không có quyền CREATE DATABASE. Không dùng
database Face thay database Core hoặc tự nâng quyền tài khoản Face.

## 1. Tạo database bằng pgAdmin

Đăng nhập server bằng postgres. Chuột phải Databases → Create → Database.
Đặt Database=core_db, Owner=postgres, rồi Save. Bước này chỉ tạo database;
không nhập SQL CREATE DATABASE cùng một transaction nhiều lệnh.

## 2. Điền file .env của Core

File nằm ngay trong core-service, không phải file của Face.

- CORE_DB_URL=jdbc:postgresql://localhost:5432/core_db
- CORE_DB_USERNAME=postgres
- CORE_DB_PASSWORD: mật khẩu postgres dùng trong pgAdmin.
- JWT_SECRET: cùng giá trị ROOMIX_JWT_SECRET của nhóm, ít nhất 32 byte UTF-8.
- FACE_SERVICE_TOKEN: cùng giá trị ở Face.
- TELEGRAM_BOT_TOKEN/CHAT_ID: cùng bot/chat nếu muốn dùng chung.
- CORE_PORT=18081 để khớp CORE_URL=http://localhost:18081 của Face hiện tại.
- CORE_ALLOWED_ORIGINS=http://localhost:3000,http://127.0.0.1:3000 nếu gọi Core trực tiếp.

Không đưa mật khẩu/token lên chat hoặc Git. File .env đã được ignore.
Spring Boot không tự đọc .env; script run_local.ps1 nạp nó cho tiến trình con.
Chỉ dùng KEY=value, mỗi biến một dòng; hỗ trợ cặp nháy ngoài đơn/kép.
Không thực thi $(), backtick hoặc nội suy biến; không hỗ trợ giá trị nhiều
dòng hoặc comment ở cuối giá trị. Biến đã đặt trong terminal được ưu tiên.
Biến do script nạp được phục hồi khi dừng, nên sửa file có hiệu lực ở lần chạy sau.

## 3. Kiểm tra chỉ đọc

Trong PowerShell tại core-service:

```powershell
.\scripts\run_local.ps1 -CheckOnly
```

Lệnh dùng Java/JDBC kiểm tra cấu hình, kết nối, ba bảng cần có và số Admin
ACTIVE nếu bảng đã đủ. Không khởi chạy Spring, tạo bảng/tài khoản, cấp JWT,
gọi Core/Face hoặc gửi Telegram. SQLSTATE=3D000 nghĩa là chưa có database;
28P01 thường là sai mật khẩu. CONFIG_MISSING liệt kê tên biến cần điền.

Nếu bảng thiếu và CORE_DB_INIT_MODE=never, SCHEMA_MISSING sẽ chặn khởi chạy.
Nếu bật always rõ ràng, SCHEMA_PENDING chỉ thông báo bảng sẽ được Core tạo
khi chạy thật; CheckOnly không tạo bảng. ACTIVE_ADMINS=0 nghĩa chưa có tài
khoản để đăng nhập, không phải đã kiểm thử đăng nhập thành công.

## 4. Khởi tạo bảng và Admin đầu tiên

Chỉ dành cho core_db mới vừa tạo. Trong .env của Core đặt:

```dotenv
CORE_DB_INIT_MODE=always
CORE_BOOTSTRAP_ADMIN=true
CORE_ADMIN_USERNAME=roomix_admin
CORE_ADMIN_PASSWORD=MAT_KHAU_BAN_TU_DAT
CORE_ADMIN_EMAIL=EMAIL_CUA_BAN
CORE_ADMIN_FULL_NAME=Quan tri Roomix
CORE_TELEGRAM_WORKER_ENABLED=false
```

Thay các giá trị viết hoa, không dùng nguyên mật khẩu ví dụ. Xem ADMIN_AUTH.md
về quy tắc mật khẩu/tài khoản. worker=false tạm dừng gửi Telegram khi chuẩn bị
database; không làm mất hàng đợi nếu bot/chat đã được cấu hình.

Sau đó chạy:

```powershell
.\scripts\run_local.ps1
```

Core sẽ áp dụng ba schema.sql/face-sessions.sql/telegram-outbox.sql và tạo
Admin bằng BCrypt. Không ghi đè tài khoản cũ. Sau khi startup thành công,
Ctrl+C rồi đặt CORE_DB_INIT_MODE=never, CORE_BOOTSTRAP_ADMIN=false và xóa giá
trị CORE_ADMIN_PASSWORD khỏi file. Chạy lại CheckOnly rồi chạy service.
Đổi CORE_TELEGRAM_WORKER_ENABLED=true khi sẵn sàng thử thông báo thật.

Nếu bootstrap báo tài khoản tồn tại, không tạo lại/đổi mật khẩu tự động;
tắt bootstrap. .env nạp theo terminal, nên nếu có biến cũ đặt bằng $env:...
hãy sửa/xóa biến đó hoặc mở terminal mới trước khi thử lại.

Script tìm driver PostgreSQL trong cache Maven của người dùng. Chưa có thì
chạy .\mvnw.cmd -DskipTests compile một lần. Có thể truyền -JavaHome hoặc
-PostgresDriver khi cài JDK/cache ở vị trí khác.

Frontend gọi Core trực tiếp chỉ thử được mật khẩu; Core không proxy Face.
Nối đầy đủ frontend/Face cần Gateway ở chặng cấu hình tiếp theo.
