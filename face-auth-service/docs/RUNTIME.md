# Kiểm tra môi trường chạy AI thật

`.venv` trước đây dùng cho unit test, hiện là Python 3.12 và không có đầy đủ
PyTorch/FaceNet. `.venv-runtime` là môi trường Python 3.11 chạy AI thật. Đây
là hai môi trường cài thư viện, không phải hai bản Face Auth.

## Cài đặt trên Windows

```powershell
.\scripts\setup_runtime.ps1
```

Nếu Python 3.11 nằm ở nơi khác:

```powershell
.\scripts\setup_runtime.ps1 -PythonExecutable 'D:\Python311\python.exe'
```

Script chỉ cài vào `.venv-runtime` trong dự án, không cài vào Python hệ
thống. Cần mạng để tải packages, bản CPU của PyTorch khoảng 200 MB và trọng
số ở lần chạy mô hình đầu tiên. Không cần CUDA cho bộ chạy thử này.

`constraints-runtime.txt` cố định PyTorch 2.2.2, torchvision 0.17.2, NumPy
1.26.4, Pillow 10.2.0, OpenCV 4.11.0.86 và Ultralytics 8.4.184 cho
facenet-pytorch 2.6.0.
Cặp torch/torchvision và nguồn wheel CPU đối chiếu với
[hướng dẫn PyTorch](https://pytorch.org/get-started/previous-versions/#v222).
Đây là bộ tương thích với pipeline hiện tại; không tự đổi một gói lên bản
mới nhất mà chưa kiểm tra lại toàn bộ dependencies và inference.

## Khởi chạy hằng ngày

Sau khi tạo role/database theo `DATABASE.md`, sao chép `.env.example` thành
`.env` và điền URL PostgreSQL, URL Core, token nội bộ cùng Core. Không dùng
database tạm trong `.tmp/postgres-test` làm database chạy thường xuyên.

```powershell
Copy-Item .env.example .env
# Sửa .env bằng trình soạn thảo, không gửi mật khẩu vào chat.
.\scripts\run_local.ps1 -CheckOnly
.\scripts\run_local.ps1 -Demo
```

Chỉ sao chép mẫu khi chưa có `.env`, tránh ghi đè cấu hình đã điền.
`-CheckOnly` chỉ kiểm tra cấu hình và `SELECT 1` với PostgreSQL; không tạo
bảng, không nạp AI, không gọi Core hoặc gửi Telegram. Script không tạo
database/role. Khởi chạy service mới tạo bảng theo schema hiện tại.

Script dùng `.venv-runtime`, nạp `.env` dưới dạng dữ liệu bằng python-dotenv
(đã có trong uvicorn[standard]). Biến môi trường có sẵn được ưu tiên hơn
`.env`. `-Demo` bật trang thử cho tiến trình chạy này; sau khi dừng, biến môi
trường được phục hồi. Service chỉ nghe `127.0.0.1:8003`. Dừng bằng Ctrl+C.

Mở `http://localhost:8003/demo` sau khi service khởi động. Thử LOGIN/ENROLL
cần phiên Core thật theo `CAMERA_DEMO.md`; kiểm tra database đạt chưa có
nghĩa là đã tích hợp Core thành công.

## Kiểm tra thư viện và mô hình

```powershell
# Chỉ kiểm tra metadata các gói đã cài, không tải mô hình.
.\.venv-runtime\Scripts\python.exe -m src.face_auth.check_runtime

# Kiểm tra xung đột dependencies.
.\.venv-runtime\Scripts\python.exe -m pip check

# Load YOLO, MTCNN, FaceNet; chạy inference trên tensor và ảnh trống.
.\.venv-runtime\Scripts\python.exe -m src.face_auth.check_runtime --models

# Tùy chọn: phân tích một ảnh cục bộ, không đăng ký hoặc lưu hồ sơ.
.\.venv-runtime\Scripts\python.exe -m src.face_auth.check_runtime --image 'C:\duong-dan\anh.jpg'
```

Metadata OK chỉ có nghĩa đã cài đủ gói. `--models` kiểm tra encoder chạy ra
vector chuẩn hóa 512 chiều và detector không báo mặt/người trên ảnh trống.
Kết quả này không chứng minh độ chính xác nhận dạng hoặc chống giả mạo.

`--image` dùng cùng `FaceService.analyze()` của API thật. Chỉ in trạng thái,
hướng đầu hoặc lý do từ chối, không in vector hoặc gửi Telegram. Không mở
camera, gọi Core, tạo tài khoản hay lưu dữ liệu vào PostgreSQL.

Nếu báo `MODELS_FAILED`, kiểm tra imports bằng môi trường runtime và kết
nối mạng tải trọng số. Lệnh chẩn đoán không in nguyên exception để tránh
đưa dữ liệu nhạy cảm vào báo cáo. Lỗi inference phải được xử lý trước khi
chạy demo; không bỏ qua liveness hoặc dùng fake trong ứng dụng thật.

## Vị trí trọng số

- YOLO được tìm trong `models/`, thư mục hiện tại, rồi `legacy/cv/`.
- Chưa có trọng số: tải model YOLO mặc định vào `models/`.
- `TORCH_HOME` mặc định là `models/torch/`.
- `YOLO_CONFIG_DIR` mặc định là `.tmp/ultralytics/`.
- Nếu các biến cache đã được cấu hình, ứng dụng tôn trọng giá trị đó.

Trọng số/cache và virtualenv không được commit vào Git. Chỉ tải trọng số
từ nguồn bạn tin cậy. Không tự tải ảnh người để đăng ký hồ sơ Admin.

## Kết quả kiểm tra trên máy dự án

Ngày 23/10/2026, Python 3.11.0 và PyTorch 2.2.2+cpu: `pip check` không có
xung đột; lệnh `check_runtime --models` trả OK. YOLO, MTCNN, FaceNet nạp
được; encoder trả vector chuẩn hóa 512 chiều; ảnh trống bị detector từ chối.
Chưa thử ảnh người thật, webcam hoặc đánh giá độ chính xác/liveness thực tế.

80 kiểm thử Python đạt trên chính môi trường runtime với PostgreSQL thật.
Khởi tạo API bằng các mô hình thật và PostgreSQL tạm thành công: `/health`
và `/demo` trả 200. Khi Core không khả dụng, tạo challenge trả 503 thay vì
bỏ qua xác thực. PostgreSQL tạm được tắt sau kiểm tra; chưa tích hợp Core
Java thật hoặc đăng ký khuôn mặt người dùng.
