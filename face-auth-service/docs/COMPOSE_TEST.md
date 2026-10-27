# Kiểm tra Core + Gateway + Face trong Compose

Chạy từ `face-auth-service`, với Docker Desktop hoạt động và model cache đã
có trong `models/`. Bài kiểm tra dùng `Roomix_Backend/compose.yml` của nhóm,
bao gồm Kafka và hai PostgreSQL. Không kiểm tra nghiệp vụ Rental/RAG.

## Chuẩn bị image

```powershell
docker build -t roomix-face-auth:local .
docker build -f ../Dockerfile --build-arg SERVICE=core-service -t roomix-core-admin-test:local ..
docker build -f ../Dockerfile --build-arg SERVICE=api-gateway -t roomix-gateway-admin-test:local ..
```

## Chạy kiểm tra

```powershell
.\.venv-runtime\Scripts\python.exe scripts\check_compose.py
```

Đã sử dụng Docker Compose 5.1.4. Override của bài test dùng các thẻ YAML
`!override` và `!reset`; Docker Compose cũ không hỗ trợ sẽ báo lỗi ở bước config.

Script tạo project `roomix_admin_smoke_<random>`; dữ liệu, mật khẩu, JWT key
và service token đều dành riêng cho test. Chỉ Gateway công bố một cổng loopback
ngẫu nhiên; không lấy các cổng local 3000/8003/18080/18081 hoặc cổng database.
Telegram bị tắt. Model cache được mount chỉ đọc, không sao chép hồ sơ khuôn mặt
của người dùng vào database thử.

Gateway chỉ bỏ dependency Rental/RAG trong override tạm. Kafka và các
dependency PostgreSQL của Core/Face giữ theo Compose gốc. Script kiểm tra
volume đều có prefix project thử và không phải external volume.

## Phạm vi xác minh

1. Flyway V1–V8 trên Core database mới, đăng nhập mật khẩu, JWT và `/api/users/me`
   đi qua Gateway; ENROLL thiếu JWT bị từ chối.
2. Face app thật tải AI CPU, kiểm tra ảnh trống, callback lỗi về Core; đăng
   nhập không đạt không được cấp JWT. Callback internal không public qua Gateway.
3. Chuyển riêng container Face sang module `tests/compose/compose_face_fixture.py`
   bằng mount chỉ đọc và command override. Analyzer giả cho phép kiểm tra
   đầy đủ luồng thành công mà không dùng ảnh sinh trắc học thật:
   ENROLL lưu UUID/vector → LOGIN nhận dạng → Core cấp JWT → đọc profile.
4. Kiểm tra complete lặp không tạo token khác, JWT FACE không được ENROLL,
   mặt không khớp bị từ chối, Tenant không được đăng ký mặt, Admin khóa bị từ chối.
5. Kiểm tra không có thông báo trong hai outbox khi Telegram tắt.

Module fixture không được COPY vào Docker image và không được bật bằng biến
môi trường trong ứng dụng chính. Mặc định image luôn chạy Face app thật.
Kết quả analyzer giả chứng minh giao tiếp/phân quyền/lưu trữ, không đo độ
chính xác AI hoặc khả năng chống giả mạo. Webcam thật đã được người dùng
kiểm tra riêng trên bộ local.

## Dữ liệu và dọn dẹp

Tài khoản `compose_admin`/`compose_tenant` và mật khẩu cố định trong script
chỉ được tạo trong database tạm. Không dùng làm tài khoản triển khai.
Script luôn chạy `compose down --volumes` cho đúng project thử có tên ngẫu
nhiên sau khi xong, kể cả test thất bại. Image và build cache giữ lại để dùng
lần sau. Log nằm trong `.tmp/roomix_admin_smoke_<random>/compose.log`.

Nếu tiến trình bị tắt cưỡng bức trước khi `finally` chạy, tìm đúng tên project
thử bằng `docker compose ls` và kiểm tra các container/volume trước khi dọn.
Không dùng `docker compose down -v` trên project thật của nhóm.
