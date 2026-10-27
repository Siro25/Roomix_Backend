# Đăng nhập Admin qua Core

Core dùng AuthController, AuthServiceImpl, SecurityConfig và JwtTokenProvider
của nhóm trong com.team4.core. Không có controller hoặc bộ ký JWT Admin riêng.
Đăng nhập mật khẩu không gọi Face, không yêu cầu đã đăng ký khuôn mặt.

- POST /api/auth/login nhận username/password; trả ApiResponse.data chứa
  accessToken, tokenType, expiresIn và user.
- GET /api/users/me đọc lại tài khoản; frontend chỉ chấp nhận ADMIN ACTIVE.
- JWT HS256 có sub UUID, role, aud=roomix-api và auth_method=PASSWORD/FACE.
- JWT_SECRET là Base64 của ít nhất 32 byte, dùng chung với Core/Rental của nhóm.
- Tài khoản bị khóa không được dùng để tạo/hoàn tất phiên Face.

users là bảng dùng chung các role TENANT, LANDLORD, ADMIN. Không tạo bảng Admin
hoặc tài khoản qua endpoint công khai riêng. Việc tạo Admin đầu tiên cần phối
hợp người phụ trách Core; không còn BootstrapAdmin/CORE_BOOTSTRAP_ADMIN.

Flyway là cơ chế tạo bảng duy nhất; không dùng CORE_DB_INIT_MODE hoặc chạy
schema.sql cũ. Xem MAIN_INTEGRATION.md, LOCAL_SETUP.md, FACE_AUTH.md và TELEGRAM.md.

Kiểm thử Core dùng TEST_CORE_DB_URL với schema core_auth_test_<uuid> riêng;
không chạy bộ test trên schema ứng dụng. HTTP Telegram trong test được mock.
Kết quả kiểm tra thật cập nhật ở ../../face-auth-service/docs/LOCAL_ACCEPTANCE.md.
