# Core tích hợp Face Python

Core quản lý phiên/tài khoản/JWT. Face Python quản lý ảnh, liveness, nhận
dạng và embedding; Core không nhận/lưu ảnh hoặc chạy mô hình AI.

## API

| Endpoint POST | Ai gọi | Dữ liệu và kết quả |
|---|---|---|
| `/api/auth/face/sessions` | Frontend | `{purpose: LOGIN hoặc ENROLL}` → authSessionId, purpose, expiresAt, completionToken |
| `/internal/face/sessions/validate` | Face | `{authSessionId,purpose}` → accepted, adminId, expiresAt |
| `/internal/face/results` | Face | `{authSessionId,purpose,verified,adminId,reason}` → accepted |
| `/api/auth/face/complete` | Frontend | `{authSessionId,completionToken}` → AuthResponse JWT |

expiresAt là Unix seconds. Mỗi phiên tối đa 90 giây; ENROLL không vượt thời
hạn JWT mật khẩu. LOGIN chưa có adminId lúc tạo; nhận dạng thành công mới
gắn Admin từ callback Face. Core chỉ chấp nhận ID UUID của tài khoản Core.

Tạo ENROLL và validate ENROLL yêu cầu cùng Bearer JWT đã đăng nhập PASSWORD,
tài khoản ADMIN ACTIVE. JWT FACE không được đăng ký/thay mặt. Core kiểm tra
claim do mình ký và DB, không tin cờ method/adminId của frontend. ENROLL
không có completionToken và không thể dùng để nhận JWT đăng nhập.

Hai API nội bộ bắt buộc `X-Service-Token`, kể cả khi có Bearer JWT Admin.
FACE_SERVICE_TOKEN bên Core phải giống Face Python và ít nhất 32 byte UTF-8.
Filter kiểm tra trước khi đọc body; cấu hình này không dùng JWT_SECRET
làm token nội bộ. Không đưa route `/internal/**` ra Gateway công khai.

## Bảo vệ phiên và thử lại

LOGIN tạo completionToken 32 byte ngẫu nhiên. Core chỉ lưu SHA-256 của mã;
frontend chỉ giữ trong bộ nhớ. Mã không đi sang Face hoặc xuất hiện trong
validate. Chỉ authSessionId không đủ nhận JWT. Không log body có mã/JWT.

Session có trạng thái WAITING, VERIFIED hoặc FAILED. Callback phải đến sau
validate và trước hết hạn. Callback thành công cần UUID Admin, reason=null;
thất bại cần adminId=null và mã reason. Core kiểm tra quyền/khóa và chủ sở
hữu ENROLL. `accepted=true` cho callback thất bại chỉ có nghĩa đã ghi nhận
kết quả; trạng thái FAILED không thể cấp JWT.

Callback đầu tiên lưu fingerprint payload và quyết định trong transaction.
Gửi lại cùng payload trả lại quyết định cũ; đổi payload trả 409. Mỗi phiên
được khóa bằng SELECT FOR UPDATE để các request đồng thời không ghi đè nhau.

Complete chỉ chấp nhận LOGIN VERIFIED, đúng bí mật và còn hạn; Core đọc lại
trạng thái tài khoản. JWT FACE có auth_method=FACE và jti gắn phiên. Core
lưu thời điểm cấp/hết hạn, không lưu JWT rõ trong DB; gửi lại complete trong
thời hạn phiên tái tạo cùng JWT với cấu hình khóa/issuer không đổi. expiresIn
trả thời gian còn lại, không kéo dài JWT khi retry. Không tạo lần đăng nhập
mới chỉ vì retry. Tài khoản bị khóa/mất quyền/xóa vẫn bị từ chối ở complete.

Đăng nhập mật khẩu có jti ngẫu nhiên riêng cho mỗi lần cấp. Ràng buộc ENROLL
lưu hash JWT gốc để cùng tài khoản nhưng JWT khác cũng không chiếm phiên.

ENROLL callback accepted chưa chứng minh Face đã lưu embedding. Frontend
chỉ báo đăng ký thành công sau response SUCCEEDED cuối từ Face. Nếu Face
lỗi lưu DB, retry cùng kết quả; chưa có transaction phân tán giữa hai service.

## Cập nhật database và cấu hình

Thêm FACE_SERVICE_TOKEN vào environment Core, dùng giá trị đang có trong
`.env` Face Python. Spring Boot không tự nạp .env. Không ghi bí mật vào Git.

Database Core tạo bảng admin_face_sessions bằng Flyway V7; V8 tạo outbox
Telegram. Không chạy face-sessions.sql hoặc CORE_DB_INIT_MODE của bộ khung cũ.

Core giữ metadata phiên/hash, không có dữ liệu sinh trắc. Có thể dọn phiên
hết hạn hơn một ngày trong database Core:

```sql
DELETE FROM admin_face_sessions
WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '1 day';
```

Phiên đã xóa không thể dùng lại. Chặng này chưa tự đăng ký lịch dọn.
Các mã lỗi chính: 401 thiếu đăng nhập/token nội bộ, 403 sai quyền/chủ phiên/
mục đích, 404 không có phiên, 409 chưa xác thực hoặc đổi kết quả, 410 hết hạn.
Lỗi database trả 503, không tự coi là đăng nhập thành công.

## Kết quả kiểm tra

Core sau tích hợp main có 57 test PostgreSQL/JWT/Face/Telegram đạt. Unit test
mock HTTP Telegram. Đã kiểm tra callback, idempotency, complete đồng thời,
tài khoản khóa và JWT FACE không được ENROLL. Luồng liên service tự động dùng
analyzer giả để kiểm tra giao tiếp; lần thử webcam dùng AI thật.

Người dùng đã xác nhận đăng ký/đăng nhập webcam và Telegram thật, cả với backend
Docker. Xem face-auth-service/docs/LOCAL_ACCEPTANCE.md từ gốc backend để biết
phạm vi, kết quả và giới hạn. Chưa benchmark nhận dạng/chống giả mạo toàn diện.
