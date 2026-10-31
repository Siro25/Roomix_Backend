# Face Python trong Docker của nhóm

Compose gốc dùng Dockerfile Java cho Core/Gateway/Rental. Face có Dockerfile
Python 3.11 riêng, dùng cùng requirements và bộ AI CPU đang chạy local.
Không build Face bằng Maven.

Đã kiểm tra image Face riêng: build thành công, nạp AI CPU, kết nối PostgreSQL
tạm, health HTTP đạt và từ chối ENROLL thiếu JWT. Container chạy UID 10001,
pip check đạt. Compose tích hợp Core/Gateway/Face cùng Kafka và hai PostgreSQL
cũng đã qua bài kiểm tra riêng; xem `COMPOSE_TEST.md` cho phạm vi, fixture và
cách chạy lại. Nghiệp vụ Rental/RAG không nằm trong bài kiểm tra phần Admin này.

## Cấu hình

Trong `Roomix_Backend/.env`, giữ cấu hình của nhóm và bổ sung các biến mẫu
từ `face-auth-service/docker.env.example`. FACE_SERVICE_TOKEN phải giống
nhau ở Core và Face. JWT chỉ cấu hình cho Core và các Java service cần nó.
Không đưa `.env` thật vào Git hoặc image.

Compose tạo database Face riêng `roomix_face` trên service `face-db`;
Core vẫn dùng `core_db` của nhóm. Mật khẩu được truyền qua biến riêng, Python
tự URL-encode nên có thể chứa ký tự đặc biệt. Docker không dùng database
PostgreSQL Windows localhost:5432 của bạn.

## Chạy tại Roomix_Backend

```powershell
docker compose config --quiet
docker compose up -d --build core-service face-auth-service api-gateway
docker compose logs -f face-auth-service
```

Compose có thể khởi động thêm các dependency đã khai báo của nhóm. Lần build
đầu tải PyTorch CPU và các thư viện AI. Lần khởi động đầu tải trọng số YOLO và
FaceNet qua Internet; cache nằm trong volume `face-models`. Nếu mạng không
cho tải trọng số, cần chuẩn bị model/cache trước theo `RUNTIME.md`.

Face lắng nghe 8084 trong container, host mặc định 18084; Core nội bộ 8081,
Gateway host 18080. Frontend dùng Gateway, không dùng URL tên container.
Mỗi container Face chạy một worker để tránh tải nhiều bản mô hình.

Không chạy `docker compose down -v` nếu cần giữ dữ liệu. Người dùng đã xác nhận webcam thật
và Telegram với backend Docker. Hiệu chỉnh ngưỡng/benchmark được để sau.
