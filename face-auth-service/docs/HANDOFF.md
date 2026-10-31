# Bàn giao Face Auth Python

## Phạm vi

Tiếp tục pipeline YOLO, MTCNN, FaceNet của code ban đầu. Core Java của nhóm
quản lý users, mật khẩu, ADMIN/ACTIVE, phiên xác thực và JWT. Face Python xử lý
ảnh, liveness chuyển động mức MVP, embedding và nhận dạng Admin 1:N.

- Mật khẩu và khuôn mặt là hai cách đăng nhập độc lập.
- Đăng ký/thay mặt yêu cầu JWT PASSWORD của ADMIN ACTIVE; adminId lấy từ Core.
- Mỗi Admin có một hồ sơ mặt; đăng ký lại thành công thay mẫu cũ.
- Core và Face có PostgreSQL riêng, không đọc/ghi chéo database.
- Core là nơi duy nhất cấp JWT; Face trả kết quả qua REST nội bộ có service token.
- Core gửi chữ cho mật khẩu đúng/sai và đăng nhập mặt thành công.
- Face gửi ảnh cho đăng nhập mặt thất bại và đăng ký mặt thành công.

## Tài liệu cần đọc

| Nội dung | Tài liệu |
|---|---|
| Vị trí code | FILE_MAP.md |
| Hợp đồng REST, phiên và lỗi | CORE_INTEGRATION.md |
| PostgreSQL Face | DATABASE.md |
| Chạy Windows | RUNTIME.md |
| Docker và cấu hình | DOCKER.md |
| Kiểm thử Compose tự động | COMPOSE_TEST.md |
| Webcam với Docker | COMPOSE_WEBCAM.md |
| Quy tắc ảnh/thông báo | TELEGRAM.md |
| Kết quả nghiệm thu và giới hạn | LOCAL_ACCEPTANCE.md |

Core dùng package com.team4.core, AuthServiceImpl và JwtTokenProvider của nhóm.
Flyway V1–V11 của main giữ nguyên; V12/V13 thêm phiên Face và outbox Telegram. Không còn
bootstrap Admin hay schema.sql của bộ Core thử nghiệm cũ.
Frontend Roomix có tài liệu docs/ADMIN_AUTH_INTEGRATION.md; JWT và completionToken
chỉ giữ trong bộ nhớ. JWT FACE không được dùng để đăng ký/thay khuôn mặt.

## Trạng thái kiểm chứng

Đã kiểm tra Core/Gateway/Face với PostgreSQL riêng và Compose. Người dùng đã
xác nhận đăng ký/đăng nhập webcam thật với backend local và Docker, cùng thông
báo Telegram theo yêu cầu. Fixture chỉ nằm trong bài test giao tiếp tự động;
image và lần thử webcam dùng AI thật. Xem LOCAL_ACCEPTANCE.md để phân biệt.

Chưa có benchmark độ chính xác hoặc chống ảnh/video giả mạo toàn diện. Liveness
quay đầu là MVP. Nghiệp vụ Rental/RAG và frontend trong container nằm ngoài
phạm vi kiểm chứng Face/Admin. Không coi đăng nhập đạt là toàn dự án đã hoàn tất.

## Git và dữ liệu riêng

Không commit .env, .tmp, môi trường Python, trọng số, ảnh camera, embedding,
database dump hoặc log riêng. scripts/audit_git.py kiểm tra file tracked và
file mới của cả hai repo, chỉ báo đường dẫn/loại lỗi, không in giá trị secret.
Kết quả quét không thay thế việc review diff và thu hồi key từng lộ trong lịch sử.

Code điểm danh/bộ khung Java không dùng đã được bỏ khỏi cây source. Bản gốc
được lưu cục bộ trong .tmp/review-archive/original-unused-source.zip, bị Git bỏ
qua; pipeline hiện tại vẫn phát triển từ code AI ban đầu. Không commit/push tự động.

## Lệnh cho người mới pull code trên Windows

Các bước dưới đây dành cho máy mới, không phụ thuộc đường dẫn, tài khoản,
container test hoặc thư mục .tmp trên máy tác giả. Backend và frontend là hai
repository riêng: phải lấy đủ thay đổi của cả hai. Những thay đổi chưa commit/
push trên máy tác giả chưa thể lấy bằng pull. Không gửi .env hay database mặt
thật qua Git. Đường dẫn D:\Roomix dưới đây chỉ là ví dụ, thay bằng nơi clone.

### Cách 1: backend bằng Docker, frontend bằng Node.js

Máy cần Git, Docker Desktop đang chạy Linux containers và Node.js >=20.9.0
(yêu cầu từ phiên bản Next.js của dự án). Không cần cài Python, JDK hoặc Maven
trên máy cho cách này. Dockerfile tự cài Java 21, Python 3.11, PyTorch CPU và
các dependency backend. Frontend dùng package-lock.json, cài bằng npm ci.

Tại backend, khi working tree đã sạch và nhánh face-verification đã được push:

```powershell
cd D:\Roomix\Roomix_Backend
git switch face-verification
git pull --ff-only origin face-verification
docker version
docker compose version
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
notepad .env
```

Điền .env trước khi chạy phần tiếp theo:

| Biến | Cần điền |
|---|---|
| ROOMIX_DB_PASSWORD | Mật khẩu database của bộ Docker trên máy này |
| ROOMIX_JWT_SECRET | Key Base64 của ít nhất 32 byte; dùng key do người phụ trách Core thống nhất |
| FACE_SERVICE_TOKEN | Bí mật nội bộ ít nhất 32 byte; Compose truyền cùng giá trị sang Core và Face |
| TELEGRAM_BOT_TOKEN, TELEGRAM_CHAT_ID | Cùng điền hoặc cùng để trống; lấy cấu hình riêng qua kênh riêng nếu cần |
| CORE_TELEGRAM_WORKER_ENABLED | true nếu muốn Core gửi thông báo chữ, mặc định false |
| CLOUDINARY_*, GEMINI_API_KEY | Theo cấu hình phần việc của nhóm; không cần cho nhận dạng Face |

Không dán nguyên tên placeholder vào ô secret. Không dùng JWT_SECRET thay cho
FACE_SERVICE_TOKEN. Bỏ trống cả bot/chat sẽ tắt Telegram. Giữ cổng mặc định nếu
chưa cần thay đổi. Nếu đang chạy bộ Docker thử cũ hoặc service local trên máy,
dừng bộ đó trước để tránh trùng cổng/Kafka consumer.

```powershell
# Vẫn tại Roomix_Backend, sau khi đã điền .env.
docker compose config --quiet
docker compose --parallel 1 build
docker compose up -d --no-build
docker compose ps
docker compose logs --tail 50 face-auth-service
```

Build lần đầu cần mạng và có thể lâu vì PyTorch/model. Face tải trọng số công
khai ở lần khởi động đầu; volume face-models giữ cache. PostgreSQL tạo database
riêng; Core chạy Flyway khi startup. Không phải chạy pip install hoặc Maven
thêm trên Windows khi dùng Docker. Cấu hình đúng không đồng nghĩa đã nghiệm
thu mọi nghiệp vụ Rental/RAG; kết quả Face được ghi tại LOCAL_ACCEPTANCE.md.

Mở terminal thứ hai tại frontend; checkout nhánh frontend mà nhóm đã nhận thay
đổi Admin/Face trước khi pull (không mặc định rằng main đã có thay đổi đó):

```powershell
cd D:\Roomix\Roomix
git pull --ff-only
node --version
npm --version
npm ci
if (-not (Test-Path .env.local)) { Copy-Item .env.example .env.local }
notepad .env.local
```

Đặt NEXT_PUBLIC_API_URL=http://localhost:18080 trong .env.local nếu Gateway
dùng cổng mặc định, rồi chạy:

```powershell
npm run dev
```

Mở http://localhost:3000/admin/login trên máy đó. Máy/database mới không có sẵn
tài khoản hoặc mẫu mặt của tác giả: người phụ trách Core phải cấp tài khoản
ADMIN ACTIVE. Đăng nhập mật khẩu trước rồi đăng ký mặt của người được cấp.
Không có tài khoản mặc định hoặc bootstrap Admin trong luồng startup hiện tại.
Script change_docker_admin.py chỉ đổi tài khoản của bộ webcam test đã có trạng
thái local; không phải công cụ tạo Admin trên máy mới.

Lần sau chạy (không đổi code/dependencies) chỉ cần:

```powershell
# Terminal backend, tại Roomix_Backend
docker compose up -d --no-build
# Terminal frontend riêng, tại Roomix
npm run dev
```

Khi muốn dừng, Ctrl+C ở terminal frontend rồi chạy docker compose stop tại
backend. Không dùng down -v nếu cần giữ tài khoản và hồ sơ mặt. Khi pull có thay
đổi backend, build lại image; khi package-lock.json đổi, chạy lại npm ci.

### Cách 2: cài Face Python để phát triển trực tiếp

Đây là lựa chọn thay cho chạy Face trong Docker, không bắt buộc cài thêm nếu
chỉ dùng cách 1. Máy cần Python 3.11 và Windows Python launcher py. Core,
PostgreSQL Face và Gateway vẫn phải được chuẩn bị/cấu hình tương ứng; cài
thư viện Python không tự dựng đầy đủ stack.

```powershell
cd D:\Roomix\Roomix_Backend\face-auth-service
py -3.11 --version
$python311 = py -3.11 -c 'import sys; print(sys.executable)'
.\scripts\setup_runtime.ps1 -PythonExecutable $python311
.\.venv-runtime\Scripts\python.exe -m pip check
.\.venv-runtime\Scripts\python.exe -m src.face_auth.check_runtime --models
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
notepad .env
```

Điền CORE_URL, FACE_SERVICE_TOKEN và FACE_DATABASE_URL của môi trường đang dùng
(không trỏ nhầm database Core). Lưu file rồi chạy:

```powershell
.\scripts\run_local.ps1 -CheckOnly
.\scripts\run_local.ps1
```

Face local lắng nghe 8003. Nếu dùng Gateway local, đặt FACE_SERVICE_URL thành
http://127.0.0.1:8003 và CORE_SERVICE_URL theo Core đang chạy. Cấu hình mẫu Face
đặt CORE_URL=http://localhost:8081; nếu Core Docker publish 18081 thì đổi URL
đó thành http://localhost:18081. Xem RUNTIME.md, DATABASE.md và tài liệu Gateway.
Không chạy hai bản Face rồi để frontend gọi nhầm một bản.

Dependencies kiểm thử (tùy chọn, cùng .venv-runtime):

```powershell
.\.venv-runtime\Scripts\python.exe -m pip install -r requirements-test.txt -c constraints-runtime.txt
```

Nếu chạy/sửa Core hoặc Gateway trực tiếp ngoài Docker thì cài thêm JDK 21;
không cần cài Maven riêng vì repo có Maven Wrapper. Tại từng thư mục service:

```powershell
.\mvnw.cmd -DskipTests compile
```

Đây chỉ là bước tải dependency/compile; cấu hình database/JWT/Kafka khi chạy
Core xem core-service/docs/LOCAL_SETUP.md. Không tự tạo lại secret chung hoặc
thay migration của nhóm để chữa lỗi cấu hình.
