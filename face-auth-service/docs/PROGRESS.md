# Tiến độ sau khi tích hợp main

## Đã hoàn thành trong code và kiểm thử tự động

- Core dùng chung tài khoản và JWT của nhóm, có dấu PASSWORD/FACE; phiên
  ENROLL chỉ dành cho ADMIN ACTIVE đăng nhập bằng mật khẩu.
- Gateway cho đăng nhập Face trước JWT, giới hạn đúng ba thao tác challenge,
  bổ sung CORS và xử lý header CORS trùng với Core.
- Frontend đọc đúng response của Core mới; 44 test API/camera, TypeScript,
  ESLint và build đều qua. Dependency được cài tại thư mục Roomix bằng npm ci.
- Core có 57 test qua; Gateway có 20 test qua. Face có 118 test qua, gồm
  cấu hình Docker và phục hồi frame mờ; mô hình thật chạy CPU, embedding 512 chiều.
- Đã thử liên thông HTTP thật với analyzer giả: frontend API client,
  Gateway, Core, Face, PostgreSQL; các schema test đã xóa và server test đã dừng.
- core_db Windows local đã chạy Flyway V1–V8, tạo admin_face_local ACTIVE,
  kiểm tra đăng nhập mật khẩu và profile. Mật khẩu nằm trong file .tmp local.
- Core Windows local đã chuyển sang role PostgreSQL `roomix_core`, sở hữu
  database/bảng Core để Flyway hoạt động, không có quyền quản trị cluster.
  Core và Face CheckOnly đều đạt. Mật khẩu postgres chưa thay đổi.
- Dockerfile Face Python và Compose/database Face riêng đã chuẩn bị; cấu hình
  Compose hợp lệ. Đã bổ sung ignore venv, file tạm, node_modules và .env.
- Rental chỉ sửa đúng một dòng JWT để đọc biến môi trường theo sự đồng ý của
  người dùng; không thay đổi nghiệp vụ Rental.

## Đã xác nhận bằng thao tác thật

- Người dùng đã đăng ký và đăng nhập thành công bằng khuôn mặt trên webcam.
  Các cải thiện nhận dạng/liveness tiếp theo được hoãn theo yêu cầu người dùng.
- Telegram thật: Core đã ghi SENT cho mật khẩu sai, mật khẩu đúng và đăng nhập
  mặt thành công; Face đã ghi SENT cho ảnh đăng nhập mặt thất bại. Người dùng
  xác nhận nhận được thông báo mong muốn.
- Người dùng đã xác nhận nhận được ảnh Telegram khi đăng ký mặt thành công.
  Đã xác nhận đủ năm loại thông báo theo yêu cầu bằng thao tác thật.
- Core local đã lưu CORE_TELEGRAM_WORKER_ENABLED=true cho các lần chạy sau.
- Token bot và Face service token đã được người dùng thay; JWT dùng giá trị
  do người phụ trách Core cung cấp. Mật khẩu postgres chưa thay, Core dùng
  role riêng roomix_core với mật khẩu ngẫu nhiên.
- Docker Desktop từng lỗi socket runtime Inference Manager/Secrets Engine.
  Đã giữ thư mục runtime cũ làm backup và xác nhận Engine 29.5.3 hoạt động;
  không reset image/volume hoặc dữ liệu WSL.
- Image `roomix-face-auth:local` đã build thành công. Kiểm tra container thật
  với PostgreSQL/network tạm đạt: AI CPU tải được, HTTP health OK, từ chối
  ENROLL không có JWT, chạy UID 10001 không phải root và pip check đạt.
  Đã dọn tài nguyên thử.
- Compose tích hợp Core + Gateway + Face + Kafka + hai PostgreSQL đã đạt.
  Dùng Compose gốc với override project/cổng/dữ liệu thử riêng, bỏ dependency
  Rental/RAG khỏi Gateway trong bài test. Face thật xác nhận startup và ảnh
  trống; fixture chỉ dành cho test xác nhận đầy đủ ENROLL/LOGIN/callback/JWT,
  lưu hồ sơ UUID, token idempotent, từ chối Tenant/Admin khóa/mặt không khớp.
  Đã dọn container/network/volume thử. Xem `COMPOSE_TEST.md` để chạy lại.
- Đã chuẩn bị bộ Compose riêng dùng AI thật cho webcam, không mount fixture.
  Frontend local được trỏ sang Gateway Docker; tài khoản `docker_webcam_admin`
  và mật khẩu riêng nằm trong file local bị Git bỏ qua. Đã kiểm tra mật khẩu/
  profile qua Docker; người dùng đã xác nhận ENROLL/LOGIN webcam thật thành công.
  Dữ liệu Docker được giữ lại cho thao tác tay, không tự dọn volume.
  Xem `COMPOSE_WEBCAM.md`.
- Đã bật bot/chat thật và worker Telegram trong Core/Face của bộ webcam Docker
  theo yêu cầu người dùng, sau khi kiểm tra hai outbox trống. Đã khởi động lại
  riêng hai service, giữ Gateway/database/hồ sơ. Người dùng sau đó đã xác nhận
  Telegram của bộ Docker hoạt động bình thường.

## Cần người dùng phối hợp trước khi nghiệm thu

- JWT cũ từng nằm trong cấu hình Core/Rental trong lịch sử Git. Xóa giá trị
  hiện tại chưa thu hồi key cũ; không tự viết lại lịch sử hoặc force-push.
- Không nằm trong bài kiểm tra này: nghiệp vụ Rental/RAG và frontend chạy
  trong container. Webcam với backend local và Docker đều đã được người dùng
  kiểm tra; độ chính xác/liveness tiếp tục cải thiện sau.
- Benchmark độ chính xác/liveness và cải thiện camera được để sau;
  đăng ký/đăng nhập thành công một lần không phải bảo đảm chống giả mạo.

Xem `LOCAL_ACCEPTANCE.md` cho thứ tự kiểm tra và `DOCKER.md` cho Docker.
Không push code trong các bước trên; người dùng quyết định thời điểm push.
