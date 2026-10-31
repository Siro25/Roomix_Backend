# Kiểm tra Admin trên máy thật sau khi tích hợp main

## Đã xác minh tự động

- Core: 57 kiểm thử PostgreSQL, JWT, phiên Face và Telegram giả đều qua.
- Gateway: 20 kiểm thử filter, CORS và định tuyến HTTP đều qua.
- Frontend: 44 kiểm thử API/camera, TypeScript, ESLint và build đều qua.
- Liên thông HTTP: frontend API client → Gateway → Core; đăng ký/đăng nhập
  Face qua Gateway → Face Python → callback Core, với analyzer giả.
- Face: 118 kiểm thử đều qua, gồm cấu hình Docker và phục hồi frame mờ.
- AI thật trên CPU: mô hình tải được, embedding 512 chiều, từ chối ảnh trống.
- core_db local đã chạy Flyway V1–V8; tài khoản admin_face_local đã tạo và
  thử đăng nhập mật khẩu thành công. Mật khẩu chỉ lưu ở file .tmp local bị bỏ qua.

Các kiểm thử analyzer giả không chứng minh độ chính xác nhận dạng/liveness.
Người dùng đã xác nhận đăng ký và đăng nhập webcam thành công. Core/Face đã
gửi thành công bốn loại thông báo Telegram: mật khẩu sai/đúng, đăng nhập mặt
thành công và ảnh đăng nhập mặt thất bại. Người dùng cũng đã xác nhận nhận
ảnh khi đăng ký mặt thành công; đủ năm loại thông báo theo yêu cầu.
Docker Compose tích hợp Core/Gateway/Face, Kafka và hai PostgreSQL đã đạt.
Ảnh trống dùng AI thật; luồng ENROLL/LOGIN thành công dùng fixture riêng để
kiểm tra giao tiếp và phân quyền, không đo độ chính xác AI. Xem COMPOSE_TEST.md.
Sau đó người dùng đã xác nhận ENROLL/LOGIN bằng webcam thật qua backend Docker;
không dùng fixture trong lần thử webcam. Xem COMPOSE_WEBCAM.md.

## Chuẩn bị cấu hình thật

1. Thay token Telegram đã xuất hiện trong ảnh bằng BotFather; cập nhật cả
   Core và Face. Không gửi token mới vào chat hoặc commit.
2. Tạo FACE_SERVICE_TOKEN mới và đặt cùng giá trị ở hai service.
3. Thống nhất JWT_SECRET mới với nhóm: Base64 của ít nhất 32 byte ngẫu nhiên.
   Cập nhật các Java service dùng chung JWT, không chỉ Core.
4. Đổi mật khẩu PostgreSQL đã bị lộ; cập nhật các cấu hình dùng tài khoản đó.
   Tránh đổi vội nếu các ứng dụng khác vẫn đang dùng cùng tài khoản postgres.
5. Giữ CORE_TELEGRAM_WORKER_ENABLED=false trước khi muốn gửi thông báo thật.

File cấu hình local: core-service/.env và face-auth-service/.env.
Core local hiện dùng tài khoản PostgreSQL `roomix_core`, sở hữu `core_db` và
các bảng Core để chạy Flyway, không có quyền superuser/tạo database/tạo role.
Mật khẩu riêng được lưu trong Core `.env`; không dùng lại mật khẩu postgres.
Thay đổi này chỉ áp dụng PostgreSQL Windows local, không đổi tài khoản Docker.
Khi dùng Gateway, có thể để CORE_ALLOWED_ORIGINS trống; Gateway quản lý CORS.
Core local 18081, Face local 8003, Gateway 18080, frontend 3000.

## Khởi chạy local

Mỗi service dùng một terminal. Đầu tiên chạy -CheckOnly ở Core và Face.
Chạy Core bằng core-service/scripts/run_local.ps1, Face bằng
face-auth-service/scripts/run_local.ps1. Xem api-gateway/docs/FACE_ADMIN.md
để chạy Gateway với URL local đúng. Tại thư mục frontend Roomix chạy `npm run dev`.
Mở `http://localhost:3000/admin/login`.

## Các thao tác cần người dùng kiểm tra

1. Đăng nhập admin_face_local bằng mật khẩu. Camera không được yêu cầu.
2. Chọn Đăng ký khuôn mặt, cho phép camera, làm theo thử thách quay đầu.
   Hồ sơ phải gắn đúng tài khoản đang đăng nhập, không nhập adminId thủ công.
3. Đăng xuất, chọn đăng nhập bằng khuôn mặt đã đăng ký.
4. Kiểm tra khuôn mặt chưa đăng ký, hai khuôn mặt, ảnh thiếu sáng, ảnh tĩnh,
   dừng camera và phiên hết hạn. Không coi tất cả thử nghiệm là chống giả mạo tuyệt đối.
5. Khi sẵn sàng thử Telegram, bật worker Core và khởi động lại service.
   Core gửi chữ cho đăng nhập mật khẩu và Face thành công; Face gửi ảnh cho
   đăng nhập Face thất bại và đăng ký mặt thành công. Không gửi ảnh sai mật khẩu.

Lưu ý: Face có thể gửi các ảnh pending còn hạn trong outbox khi bật bot.
Liveness quay đầu hiện là MVP; cần thử người thật/ảnh/video để hiệu chỉnh.
Các trang nghiệp vụ Admin chưa nối backend vẫn có thể dùng dữ liệu mock;
không coi kiểm thử đăng nhập là đã hoàn thiện quản lý người dùng/phòng.

## Git

Không commit .env, .tmp, node_modules, venv hoặc dữ liệu khuôn mặt. Các secret
đã nằm trong lịch sử Git cần được thu hồi/thay mới; một commit xóa giá trị
không làm mất lịch sử. Không tự force-push hoặc viết lại lịch sử của nhóm.
Chỉ push nhánh face-verification sau khi kiểm tra cuối; không merge main của nhóm.
