> Tài liệu dưới đây mô tả bản webcam/điểm danh ban đầu. Web backend đang được phát triển tiếp thành Face Auth. Xem README.md ở thư mục gốc để chạy đúng phiên bản hiện tại.

# Hệ thống xác minh và nhận diện khuôn mặt

Hệ thống Python độc lập dùng webcam, có dashboard quản lý người dùng, đăng ký khuôn mặt, nhận diện nhiều người, xác minh danh tính và gửi cảnh báo **kèm ảnh qua Telegram Bot API**.

## Chạy ứng dụng web

Khuyến nghị Python 3.10 hoặc 3.11 để tương thích các thư viện AI.

```powershell
cd web/backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
python main.py --camera 0
```

Mở http://localhost:8000. Thêm người dùng và nhấn 📸 để đăng ký khuôn mặt. Chụp thêm mẫu ở các góc, ánh sáng khác nhau để cải thiện nhận diện. Trường `mssv` là mã định danh duy nhất, có thể dùng cho nhân viên hoặc tài khoản ở dự án khác.

Mô hình AI có thể tải trọng số ở lần chạy đầu; cần kết nối mạng. Nếu có trọng số phát hiện khuôn mặt `yolov8n-face.pt`, đặt trong `cv/` hoặc thư mục chạy. Khi thiếu, pipeline hiện có dùng `yolov8n.pt` để tìm vùng người rồi MTCNN trích xuất mặt. FaceNet tạo vector 512 chiều; ngưỡng so khớp mặc định 0.80.

## Telegram: cảnh báo kèm ảnh

Tạo bot với @BotFather, bắt đầu cuộc trò chuyện với bot và lấy chat ID của nơi nhận. Thiết lập trước khi chạy server:

```powershell
$env:TELEGRAM_BOT_TOKEN = "TOKEN_CUA_BAN"
$env:TELEGRAM_CHAT_ID = "CHAT_ID_CUA_BAN"
python main.py --camera 0
```

Hoặc điền vào `cv/security_config.json`:

```json
{
  "telegram_bot_token": "TOKEN_CUA_BAN",
  "telegram_chat_id": "CHAT_ID_CUA_BAN",
  "enable_notification": true
}
```

Biến môi trường được ưu tiên khi khởi động. Khi phát hiện khuôn mặt chưa đăng ký, ứng dụng lập tức gọi `sendPhoto` với ảnh JPEG của chính khung hình phát hiện và thời gian, không chờ nhiều lần xuất hiện. Cảnh báo tự động cách nhau tối thiểu 5 giây để tránh gửi liên tục; vẫn cảnh báo khi người quen và người lạ cùng xuất hiện. Thời gian đến Telegram phụ thuộc mạng và thời gian xử lý AI. Thiếu token/chat ID sẽ bỏ qua gửi; dashboard báo trạng thái gửi thực tế. Xác minh thất bại qua API cũng gửi ảnh, mỗi yêu cầu một lần.

## API để tích hợp dự án khác

| Phương thức | Đường dẫn | Chức năng |
|---|---|---|
| GET / POST | `/api/users` | Liệt kê / tạo người dùng (`name`, `mssv`) |
| PUT / DELETE | `/api/users/{id}` | Sửa / xóa người dùng |
| POST | `/api/users/{id}/enroll` | Đăng ký mẫu khuôn mặt từ webcam |
| POST | `/api/users/{id}/verify` | Xác minh 1:1 với danh tính được chỉ định |
| GET | `/api/stream` | Video MJPEG có khung nhận diện |
| GET | `/api/snapshot` | Ảnh JPEG hiện tại |
| GET | `/api/attendance/today` | Nhật ký nhận diện hôm nay |
| GET | `/api/attendance/history?days=7` | Lịch sử nhận diện |
| GET / PUT | `/api/security/config` | Cấu hình Telegram |
| GET | `/api/status` | Trạng thái webcam, nhận diện và thông báo |
| WS | `/ws` | Sự kiện `attendance`, `security_alert` |

Ví dụ xác minh với webcam: `POST /api/users/1/verify`, trả về `user_id`, `verified`, `confidence` (0–1), `notification`. Ảnh phải có đúng một khuôn mặt; người dùng phải có mẫu đăng ký. Nhận diện 1:N chạy tự động trong vòng lặp nền. Nhật ký giữ giá trị `GRANTED` / `DENIED` cho kết quả khớp / không khớp. Các đường dẫn `attendance` và trường `mssv` được giữ để tương thích dữ liệu cũ.

Đây là so khớp khuôn mặt; pipeline hiện chưa có kiểm tra chống giả mạo bằng ảnh/video. Các API hiện chưa có xác thực truy cập, bao gồm API cấu hình Telegram: triển khai trong môi trường tin cậy hoặc bổ sung xác thực khi tích hợp.

## Bản chạy độc lập

```powershell
cd cv
python -m pip install -r requirements.txt
python main.py --enroll
python main.py --camera 0
python main.py --manage
```

Bản độc lập giữ pipeline YOLOv8 + MediaPipe + FaceNet, quản lý mẫu bằng `user_database.pkl`, nhật ký nhận diện và cảnh báo ảnh Telegram. Nhấn `q` để thoát. Chạy từ `cv/` để dùng đúng cấu hình và đường dẫn dữ liệu.

## Cấu trúc

- `web/backend/face_service.py`: phát hiện, embedding, nhận diện 1:N và xác minh 1:1.
- `web/backend/camera_service.py`: đọc webcam trực tiếp.
- `web/backend/security_service.py`: gửi ảnh Telegram bất đồng bộ.
- `web/backend/main.py`: API, video, WebSocket và vòng lặp nhận diện.
- `web/frontend/`: dashboard camera, người dùng và lịch sử.
- `cv/`: bản độc lập, cấu hình Telegram và pipeline AI có thể tái sử dụng.

Backend dùng SQLite. Di chuyển dữ liệu cũ từ `cv/user_database.pkl` bằng `python migrate_pkl.py` trong `web/backend/`. Cơ sở dữ liệu của bản web và bản độc lập được lưu riêng.

## Kiểm tra

```powershell
python -m compileall -q web/backend cv
python -m unittest discover -s tests -v
```

Kiểm thử Telegram dùng giả lập HTTP và encoder ảnh, không gửi thông báo thật hay tải mô hình.
