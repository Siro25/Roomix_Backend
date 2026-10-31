# Thử webcam thật với backend Docker

Bộ thử dùng Compose gốc và các image Core/Gateway/Face đã build. Core, Face,
Kafka và hai PostgreSQL chạy trong Docker; giao diện Next.js chạy trên Windows.
Trình duyệt gửi ảnh tới Gateway Docker, Face trả callback tới Core trong cùng
network Docker. Đây không phải giao diện hoặc analyzer giả.

**Kết quả:** người dùng đã xác nhận đăng ký và đăng nhập bằng webcam thành công
với bộ Docker này. Dữ liệu được giữ lại. Sau bước webcam, người dùng đã đồng ý
bật bot/chat thật trong Core và Face; sau đó xác nhận Telegram hoạt động.

## Nhận biết đúng bộ thử

- Frontend: `http://localhost:3000/admin/login`.
- Tài khoản riêng: `docker_webcam_admin`; tên hiển thị `Admin Docker Webcam`.
- Mật khẩu ngẫu nhiên ở `.tmp/docker-webcam-access.txt`, không commit/chia sẻ.
- Gateway dùng cổng loopback riêng. Frontend được khởi động lại với URL đó;
  không sửa `.env` thật của frontend hoặc backend.
- Face chạy `src.face_auth.main:app`, không mount fixture `/test-tools`.
- Telegram đã bật cho bộ Docker hiện tại sau khi kiểm tra hai hàng đợi trống.
  Bot/chat được lưu trong file cấu hình local bị Git bỏ qua, không ghi vào image.
  Hồ sơ và tài khoản local không được sao chép
  sang Docker; model weights công khai được mount chỉ đọc.

## Thao tác

1. Tải lại trang để xóa JWT local còn trong bộ nhớ. Đăng nhập bằng tài khoản
   Docker ở trên, kiểm tra tên hiển thị là Admin Docker Webcam.
2. Đăng ký khuôn mặt và hoàn thành thử thách. Chờ thông báo thành công.
3. Đăng xuất, chọn đăng nhập bằng khuôn mặt đã đăng ký.
4. Xác nhận vào được trang quản trị và đúng tài khoản Docker.

Nếu lỗi, gửi thông báo lỗi; không cần gửi ảnh mặt, mật khẩu hoặc token. Có thể
đối chiếu metadata trạng thái trong hai database Docker mà không đọc ảnh/vector.
Đăng ký/đăng nhập một lần thành công không thay thế benchmark chống giả mạo.

## Dữ liệu khi dừng thử

Bộ thử có project và volume riêng. Trạng thái/cấu hình để tiếp tục hoặc dừng
nằm trong `.tmp/docker-webcam-state.json` và thư mục project tương ứng.
Không tự chạy `down --volumes` cho bộ webcam khi còn cần hồ sơ vừa đăng ký.
Dừng container bằng `compose stop` sẽ giữ dữ liệu. Database PostgreSQL Windows
và các hồ sơ local vẫn giữ nguyên.
