# Xác thực Admin bằng mật khẩu — Core Java

Đã triển khai `POST /api/auth/login` và `GET /api/users/me` cho ADMIN.
Không gọi Face khi đăng nhập mật khẩu. Các API tạo phiên LOGIN/ENROLL,
callback Face và hoàn tất JWT đăng nhập mặt đã có, xem `FACE_AUTH.md`.
Không sửa nghiệp vụ thuê phòng hoặc đăng ký công khai tenant/landlord.

## Cấu trúc

- `auth/AdminAccountRepository`: đọc bảng users bằng JDBC, UUID làm ID.
- `auth/AdminAuthService`: kiểm tra BCrypt, quyền ADMIN và trạng thái ACTIVE.
- `auth/JwtService`: ký JWT HS256, issuer roomix-core, subject UUID,
  claim auth_method=PASSWORD và roles=[ADMIN]. Mặc định hết hạn sau 900 giây.
- `config/AdminSecurityConfig`: xác thực chữ ký/issuer/thời hạn, yêu cầu sub
  và exp; stateless Bearer, không dùng phiên đăng nhập cookie.
- `auth/BootstrapAdmin`: tạo một Admin chỉ khi bật rõ bằng cấu hình.

POST login nhận `{username,password}`, trả trực tiếp
`{accessToken,tokenType:"Bearer",expiresIn,user}`. Profile trả
`{id,username,email,fullName,phoneNumber,role,status}`. Không trả password/hash.
Sai mật khẩu, không tồn tại, khóa hoặc sai quyền đều trả 401 UNAUTHORIZED.
GET /me đọc lại DB để từ chối tài khoản bị khóa/mất quyền sau khi cấp JWT.

SecurityFilterChain mở hai API trên và các API Face liệt kê trong FACE_AUTH.md;
các route khác bị chặn cho đến khi có module được tích hợp rõ ràng.
Không chép nguyên cấu hình này đè lên Core khác đã có nhiều API: cần merge
với SecurityFilterChain và mô hình User hiện có của người phụ trách Core.

## Database và bí mật

Database này là `core_db`, không phải `roomix_face`. Compose của nhóm map
PostgreSQL Core ra localhost:15432 mặc định, user postgres. Mật khẩu là
ROOMIX_DB_PASSWORD của Compose. CORE_DB_URL là JDBC URL, không dùng URL của
Face. JWT_SECRET dùng cùng ROOMIX_JWT_SECRET của nhóm (ít nhất 32 byte UTF-8),
khác FACE_SERVICE_TOKEN.

Spring Boot không tự đọc .env. `.env.example` chỉ liệt kê biến; truyền qua
terminal, IDE hoặc environment của Compose. Không commit bí mật.
Đã có script `scripts/run_local.ps1` để nạp .env và kiểm tra Java/JDBC trước
khi chạy. Xem LOCAL_SETUP.md cho PostgreSQL Windows cổng 5432; CheckOnly
không tạo bảng, tài khoản hoặc gửi Telegram.

`CORE_DB_INIT_MODE=always` chỉ dành cho database Core mới đã kiểm tra.
schema.sql tạo users nếu chưa có; không ALTER hoặc đổi dữ liệu bảng cũ.
Nếu users đã có ở nhánh của bạn cùng nhóm, cần đối chiếu schema trước:
UUID id; username/email duy nhất; password_hash BCrypt; full_name;
phone_number; role/status dạng chuỗi. Không tự đổi schema của thành viên khác.

## Chạy local lần đầu trên database mới

Chạy tại thư mục core-service, PostgreSQL Core phải chạy trước:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12'
$env:CORE_DB_URL = 'jdbc:postgresql://localhost:15432/core_db'
$env:CORE_DB_USERNAME = 'postgres'
$env:CORE_DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Mat khau PostgreSQL Core' -AsSecureString)).Password
$env:JWT_SECRET = [System.Net.NetworkCredential]::new('', (Read-Host 'JWT secret cua nhom' -AsSecureString)).Password
$env:FACE_SERVICE_TOKEN = [System.Net.NetworkCredential]::new('', (Read-Host 'Token noi bo cung Face Python' -AsSecureString)).Password
$env:CORE_DB_INIT_MODE = 'always'
$env:CORE_BOOTSTRAP_ADMIN = 'true'
$env:CORE_ADMIN_USERNAME = 'roomix_admin'
$env:CORE_ADMIN_EMAIL = 'admin@example.com'
$env:CORE_ADMIN_FULL_NAME = 'Quan tri Roomix'
$env:CORE_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Mat khau Admin moi' -AsSecureString)).Password
$env:CORE_ALLOWED_ORIGINS = 'http://localhost:3000'
.\mvnw.cmd spring-boot:run
```

Admin bootstrap: username 4–50 ký tự A-Z/a-z/0-9/._-, password ít nhất 8 ký tự
và tối đa 72 byte UTF-8, email/fullName bắt buộc. Không có mật khẩu mặc định.
Nếu username đã tồn tại, startup dừng và không đổi mật khẩu/quyền cũ.

Sau lần đầu thành công, Ctrl+C, tắt bootstrap và init trước khi chạy lại:

```powershell
$env:CORE_BOOTSTRAP_ADMIN = 'false'
$env:CORE_DB_INIT_MODE = 'never'
Remove-Item Env:CORE_ADMIN_PASSWORD
.\mvnw.cmd spring-boot:run
```

Chạy Core trực tiếp: mặc định localhost:8081. Để thử riêng mật khẩu, đặt
NEXT_PUBLIC_API_URL=http://localhost:8081 trong .env.local frontend rồi
khởi động lại frontend. CORE_ALLOWED_ORIGINS phải khớp origin frontend.
Đường dẫn Face không được Core này proxy, nên cấu hình trực tiếp chỉ phục vụ
thử mật khẩu. Khi tích hợp đủ các service, frontend gọi Gateway localhost:18080,
Gateway route /api/auth và /api/users đến Core, /api/face đến Face. Để trống
CORE_ALLOWED_ORIGINS khi Gateway quản lý CORS. Không đổi cổng Kafka.

Chạy Core trong Docker: dùng các biến CORE_DB_URL/USERNAME/PASSWORD và JWT_SECRET
đã có trong Compose. Chỉ bật init/bootstrap sau khi kiểm tra DB. Cổng nội bộ
8081, cổng Windows 18081 mặc định. Chặng này chưa sửa Compose hoặc Dockerfile.

## Kiểm thử

Test Java dùng PostgreSQL thật và schema riêng `core_auth_test_<32hex>`;
không dùng H2, không chạy trên bảng users sản phẩm. Thiếu TEST_CORE_DB_URL
thì test tích hợp SKIP, không được coi là đã kiểm tra database.

Tạo schema test bằng tài khoản test, đặt TEST_CORE_DB_URL dạng
`jdbc:postgresql://localhost:55433/postgres?currentSchema=core_auth_test_<32hex>`,
TEST_CORE_DB_USERNAME/TEST_CORE_DB_PASSWORD rồi chạy `.\mvnw.cmd test`.
Chỉ dùng database test riêng. Các test xóa users trong schema ngẫu nhiên đó.
Xóa đúng schema test sau khi xong. Không đặt URL database thật vào biến test.

Chưa chạy database Core của người dùng, chưa tạo Admin thật, chưa kiểm chứng
qua giao diện/Gateway thật. Thông báo Telegram chữ đã có theo TELEGRAM.md;
chưa gửi bot thật. Giới hạn thử sai ở Gateway và refresh/logout thu hồi JWT chưa triển khai.
Luồng Face Core đã có theo FACE_AUTH.md. Frontend logout hiện xóa JWT cục bộ.

Tham khảo API framework:
[Spring Security NimbusJwtEncoder](https://docs.spring.io/spring-security/reference/7.0/api/java/org/springframework/security/oauth2/jwt/NimbusJwtEncoder.html),
[Spring Boot MockMvc](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/webmvc/test/autoconfigure/AutoConfigureMockMvc.html).
