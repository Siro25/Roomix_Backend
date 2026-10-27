# Gateway cho Admin Face

Gateway chuyển `/api/auth/**` và `/api/users/**` đến Core Java, `/api/face/**`
đến Face Python. Không tạo route `/internal/face/**`; callback đi trực tiếp
từ Face đến Core bằng X-Service-Token.

## Chạy local

Core và Face phải được cấu hình riêng trước. Với Core local ở 18081,
Face local ở 8003 và frontend ở 3000, mở PowerShell trong `api-gateway`:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12'
$env:GATEWAY_PORT = '18080'
$env:CORE_SERVICE_URL = 'http://127.0.0.1:18081'
$env:CORE_SERVICE_WS_URL = 'ws://127.0.0.1:18081'
$env:FACE_SERVICE_URL = 'http://127.0.0.1:8003'
$env:GATEWAY_ALLOWED_ORIGINS = 'http://localhost:3000,http://127.0.0.1:3000'
.\mvnw.cmd spring-boot:run
```

Frontend dùng `NEXT_PUBLIC_API_URL=http://localhost:18080`. Spring Boot không
tự đọc `.env`; các lệnh trên đặt biến cho terminal hiện tại.

## Đăng nhập Face không có JWT

Chỉ cho đi tiếp ba thao tác khi chưa có Authorization:

- POST `/api/face/challenges`
- GET `/api/face/challenges/{challengeId}`
- POST `/api/face/challenges/{challengeId}/frames`

challengeId là chuỗi ngẫu nhiên 43 ký tự, cần giữ kín như thông tin phiên.
Face vẫn kiểm tra phiên do Core tạo. ENROLL cần JWT PASSWORD của Admin ACTIVE
khi tạo challenge; Gateway không quyết định danh tính hoặc cấp JWT.

Các API Admin, tài khoản và các route Face khác giữ yêu cầu Authorization.
Gateway chỉ kiểm tra sự hiện diện của header; Core xác thực JWT và quyền.

CORS dùng danh sách origin cụ thể, cho Content-Type/Authorization và hỗ trợ
credentials cho refresh cookie của Core; Face vẫn dùng Bearer JWT. Header CORS trùng từ Core được loại bỏ. Khi triển khai, cấu hình
GATEWAY_ALLOWED_ORIGINS thành địa chỉ frontend thật.

## Xác minh

Kiểm thử có máy chủ upstream giả để xác minh HTTP routing, JWT được chuyển
tiếp, preflight, origin lạ bị chặn, và callback internal không được định tuyến.
Đây chưa phải kiểm thử webcam thật hoặc gửi Telegram thật.

Sau merge main: giữ public GET /api/invoices/vnpay/ipn và /api/invoices/vnpay/return; POST vẫn cần Authorization.
