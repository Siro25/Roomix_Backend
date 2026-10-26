# Tích hợp Core từ main và Face Python

Core chạy từ `com.team4.core.CoreServiceApplication`. Chỉ dùng SecurityConfig,
AuthServiceImpl và JwtTokenProvider của nhóm. Không có controller đăng nhập hay
bộ ký JWT Admin thứ hai. Tenant/Landlord giữ luồng đăng ký, đăng nhập hiện có.

## JWT và phiên Face

JWT_SECRET là Base64 của ít nhất 32 byte, dùng chung với nhóm. JWT có role,
aud=roomix-api, sub=UUID và auth_method=PASSWORD hoặc FACE. Chỉ JWT PASSWORD
của tài khoản ADMIN ACTIVE được tạo phiên ENROLL. JWT cũ thiếu auth_method
vẫn được Core xác thực theo quy tắc của nhóm, nhưng cần đăng nhập lại bằng
mật khẩu trước khi đăng ký khuôn mặt.

API Face giữ nguyên: POST /api/auth/face/sessions, /api/auth/face/complete,
/internal/face/sessions/validate, /internal/face/results. Hai API internal
bắt buộc X-Service-Token; không công khai qua Gateway. Các API Face trả DTO
trực tiếp; API đăng nhập và /api/users/me giữ ApiResponse của nhóm.

## Database

Flyway là cơ chế tạo bảng duy nhất. Giữ nguyên V1–V6 của main, thêm V7 phiên
Face và V8 hàng đợi Telegram. Không chạy schema.sql hoặc CORE_DB_INIT_MODE=always.
Không tự baseline database đang có bảng nhưng thiếu lịch sử Flyway.
CheckOnly chỉ đọc; database trống sẽ báo SCHEMA_PENDING. Khi thực sự khởi chạy
Core, Flyway mới chạy migration. Không có chức năng bootstrap Admin cũ;
cần thống nhất cách tạo tài khoản ADMIN với người phụ trách Core.

## Telegram

Admin đăng nhập đúng bằng mật khẩu và Face: Core xếp hàng thông báo chữ.
Sai mật khẩu: thông báo cho tài khoản Admin hoặc username chưa xác định;
không gửi cho username đã biết là Tenant/Landlord. Username chưa xác thực
không được coi là danh tính Admin. Ảnh chỉ do Face gửi khi đăng nhập Face
thất bại hoặc đăng ký thành công. Worker mặc định tắt, bật có chủ ý bằng
CORE_TELEGRAM_WORKER_ENABLED=true sau khi kiểm tra bot/chat ID.

## Kiểm tra

Tests dùng schema core_auth_test_<uuid> trên PostgreSQL riêng; tắt Kafka
listener/admin auto-create, mock Telegram HTTP. Không dùng database thật.
Các test Face giữ kiểm tra quyền, khóa tài khoản, liveness-result callback,
session hết hạn, callback lặp, secret sai và complete đồng thời.

Chưa xác nhận webcam thật, gửi Telegram thật, luồng qua Gateway/Frontend
hoặc Docker. Cần kiểm tra các phần này trước khi gọi tích hợp hoàn tất.
