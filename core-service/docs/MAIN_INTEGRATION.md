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

Flyway là cơ chế tạo bảng duy nhất. Giữ nguyên V1–V11 của main, thêm V12 phiên
Face và V13 hàng đợi Telegram. Không chạy schema.sql hoặc CORE_DB_INIT_MODE=always.
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

Đã kiểm tra liên thông Gateway/Frontend và Compose Core/Gateway/Face. Người dùng
đã xác nhận webcam thật và Telegram hoạt động, cả với backend Docker. Chi tiết
và giới hạn tại face-auth-service/docs/LOCAL_ACCEPTANCE.md từ gốc backend.
Chưa đánh giá toàn bộ nghiệp vụ Rental/RAG hoặc benchmark chống giả mạo.

## Gộp main ngày 27/10/2026

Giữ chức năng reset/change password, refresh/logout và cookie của main; thêm
thông báo Admin vào AuthSession login, không thay response của controller nhóm.
Giữ các route VNPAY public GET của main cùng ba thao tác Face trước JWT.
CORS chỉ cho origin cấu hình rõ ràng; cho credentials để Core dùng refresh cookie.
Client Face/Admin hiện tại vẫn dùng JWT trong bộ nhớ, không tự bật refresh cookie.

Main đã có V7–V11 nên hai migration Face chuyển thành V12 và V13, giữ nguyên
nội dung SQL. Database mới hoặc database đang theo V1–V11 của main có thể dùng
chuỗi V1–V13. Không đổi tên migration đã áp dụng của người phụ trách Core.

**Database cũ của nhánh Face:** nếu flyway_schema_history có
V7__create_admin_face_sessions.sql hoặc V8__create_auth_telegram_outbox.sql,
không chạy Core mới trực tiếp. Preflight báo SCHEMA_FACE_LEGACY và dừng đọc-only.
Cần sao lưu và đối chiếu lịch sử trước một bước nâng cấp riêng; không xóa dữ liệu,
không chạy repair/baseline hoặc sửa checksum để bỏ qua lỗi. Lần gộp này chưa
thay đổi lịch sử migration/database thật trên Windows hoặc Docker.

Đã kiểm tra mã gộp trên cụm PostgreSQL test riêng: 59 test Core và 22 test Gateway
đạt, không skip; 13 migration áp dụng thành công. SMTP/Telegram thật không được
gọi trong kiểm thử. Không coi kết quả này là đã nâng cấp các database cũ của nhóm.
