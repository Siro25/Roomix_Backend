# Roomix Face Auth Service

Phần việc Python của AI Face: đăng ký khuôn mặt, liveness và nhận dạng
Admin. Core Java quản lý tài khoản, mật khẩu, quyền ADMIN, trạng thái khóa,
phiên xác thực và JWT. Service này **không cấp JWT và không kiểm tra mật khẩu**.

## Hai cách đăng nhập độc lập

Xem `docs/LOCAL_ACCEPTANCE.md` để biết phần đã kiểm tra, cấu hình local và
các kết quả kiểm tra webcam/Telegram thật và các bước nghiệm thu còn lại. `docs/HANDOFF.md`
mô tả hợp đồng giao tiếp và phạm vi Face.

- Mật khẩu: trình duyệt gọi Core; không gọi Face Service, không cần camera.
- Khuôn mặt: Core tạo phiên LOGIN, trình duyệt thực hiện thử thách với Face,
  Face gửi kết quả trực tiếp về Core. Core quyết định cấp JWT.
- Đăng ký mặt: Admin đã đăng nhập mật khẩu, Core tạo phiên ENROLL cho chính
  Admin đó. Face lấy `adminId` từ Core, không lấy từ body của trình duyệt.

## Tìm file theo chức năng

```text
face-auth-service/
├── src/face_auth/           Mã nguồn đang phát triển
│   ├── main.py              Điểm chạy, khởi tạo và đóng service
│   ├── config.py            Cấu hình môi trường
│   ├── admin_identity.py    Xác thực UUID và ID số cũ từ Core
│   ├── api/
│   │   ├── routes.py        Các endpoint HTTP
│   │   └── schemas.py       Kiểu request/response
│   ├── ai/
│   │   ├── face_service.py  YOLO + MTCNN + FaceNet từ code ban đầu
│   │   ├── image_input.py   Giới hạn và giải mã ảnh
│   │   ├── head_pose.py     Tính hướng quay từ landmarks
│   │   ├── liveness.py      Thử thách quay đầu
│   │   └── matching.py      Tính điểm và nhận dạng 1:N
│   ├── services/
│   │   ├── face_workflow.py Điều phối đăng ký/đăng nhập
│   │   └── telegram_worker.py Gửi hàng đợi Telegram
│   ├── db/
│   │   ├── storage.py       Lưu hồ sơ và trạng thái thử thách
│   │   ├── notification_store.py Hàng đợi ảnh Telegram
│   │   └── cleanup.py       Dọn dữ liệu tạm hết hạn
│   ├── integrations/
│   │   ├── core_client.py  Gọi Core Java
│   │   └── telegram_notifier.py Gửi ảnh Telegram
│   └── web/                Trang thử và client camera trình duyệt
├── tests/                   Kiểm thử Python, JavaScript và Compose
├── docs/                    Hợp đồng Core và bản đồ file
├── requirements.txt         Dependencies chạy ứng dụng
├── requirements-test.txt    Dependencies kiểm thử
└── .env.example             Các biến môi trường cần cấu hình
```

Điểm chạy duy nhất là `src.face_auth.main:app`, chạy từ thư mục gốc dự án.
`ai/face_service.py` là file ban đầu được phát triển tiếp và chuyển vị trí,
không phải mô hình AI được viết lại độc lập. Xem `docs/FILE_MAP.md` để tra
đường dẫn cũ và mới.

**Trạng thái hiện tại:** storage dùng PostgreSQL riêng cho Face. Đã tích hợp
Core mới từ main và kiểm tra HTTP thật qua Gateway với analyzer giả. Mô hình
thật chạy được trên CPU. Người dùng đã xác nhận đăng ký/đăng nhập webcam và
đủ năm loại thông báo Telegram hoạt động. Hiệu chỉnh độ chính xác/liveness
được để sau. Xem `docs/LOCAL_ACCEPTANCE.md` và `docs/DOCKER.md`.
Code điểm danh và bộ khung Java không dùng đã được bỏ khỏi cây source.
Bản gốc lưu cục bộ trong `.tmp/review-archive/`, không đưa vào Git.

## Chạy trên Windows

Bộ chạy AI được cố định trên Python 3.11. Xem `docs/RUNTIME.md` để phân biệt
môi trường kiểm thử `.venv` và môi trường chạy thật `.venv-runtime`.

```powershell
.\scripts\setup_runtime.ps1
.\.venv-runtime\Scripts\python.exe -m src.face_auth.check_runtime --models

$env:FACE_DATABASE_URL = 'postgresql://face_user:MAT_KHAU@localhost:5432/roomix_face'
$env:CORE_URL = 'http://localhost:8081'
$env:FACE_SERVICE_TOKEN = 'bi-mat-noi-bo-do-nhom-tu-cau-hinh'
# Tùy chọn, bỏ trống để tắt Telegram:
$env:TELEGRAM_BOT_TOKEN = 'token-bot'
$env:TELEGRAM_CHAT_ID = 'chat-id'

.\.venv-runtime\Scripts\python.exe -m uvicorn src.face_auth.main:app --host 127.0.0.1 --port 8003 --no-access-log
```

Để chạy hằng ngày: sao chép `.env.example` thành `.env`, điền cấu hình thật,
rồi chạy `./scripts/run_local.ps1 -CheckOnly` để kiểm tra PostgreSQL.
Chạy `./scripts/run_local.ps1 -Demo` để mở service phục vụ trang thử camera,
hoặc bỏ `-Demo` để dùng giá trị `FACE_ENABLE_DEMO` trong cấu hình.
Script nạp `.env`; biến môi trường có sẵn được ưu tiên. Lệnh uvicorn trực tiếp
ở trên vẫn đọc biến môi trường, không tự nạp `.env` nếu thiếu `--env-file .env`.
Lần đầu khởi động FaceNet có thể tải trọng số.
API docs: `http://localhost:8003/docs`. PostgreSQL phải chạy trước khi khởi động.
Ứng dụng tự tạo bảng theo `src/face_auth/db/schema.sql`; không tự tạo database.

**Core đã có API tích hợp; cần cấu hình database/token và chạy các service trước khi thử thật.**
Không có chế độ bỏ qua Core hoặc tự coi người gọi là Admin.

## Trang thử camera

Bật `FACE_ENABLE_DEMO=true` trước khi chạy, mở `http://localhost:8003/demo`.
Trang cần phiên thật từ Core; không có chế độ bỏ qua Core hoặc tự cấp JWT.
Xem `docs/CAMERA_DEMO.md` để thử LOGIN/ENROLL và các giới hạn đã kiểm tra.

## Cách frontend gửi camera

1. Lấy `authSessionId` từ Core cho mục đích LOGIN hoặc ENROLL.
2. Gọi `POST /api/face/challenges`. Với ENROLL, gửi kèm JWT Admin trong
   header `Authorization: Bearer ...`; Face chuyển header này cho Core xác thực.
3. Hiển thị các bước `CENTER`, `TURN_LEFT`, `TURN_RIGHT` theo phản hồi.
4. Dùng `getUserMedia` và canvas chụp JPEG. Gửi từng ảnh nhị phân đến
   `POST /api/face/challenges/{challengeId}/frames`, header `Content-Type: image/jpeg`.
5. Gửi tuần tự, khoảng 300-500 ms/ảnh, đợi response trước khi gửi tiếp.
   `IN_PROGRESS` chứa `action` cần thực hiện. Mỗi bước cần hai frame đạt, tối đa 12 giây kể từ frame đầu
   hoặc từ khi bước trước hoàn tất.
6. Khi `SUCCEEDED` hoặc `FAILED`, dừng camera. Khi LOGIN thành công,
   frontend hoàn tất đăng nhập qua Core để nhận JWT theo API của Core.

Ảnh JPEG/PNG/WebP tĩnh tối đa 1 MiB và 2.073.600 pixels.
Ảnh có EXIF orientation được xoay đúng trước khi detect. Không lật dữ liệu canvas;
`TURN_LEFT/RIGHT` được tính theo hướng trên ảnh gốc. Có thể chỉ dùng CSS để
lật phần xem trước, nhưng hướng dẫn cần phù hợp ảnh thực tế. Camera trình
duyệt cần localhost hoặc HTTPS. Frontend nên gọi qua Gateway cùng origin.

Ví dụ tạo thử thách:

```json
{"authSessionId": "ma-phien-ngau-nhien-tu-core", "purpose": "LOGIN"}
```

```json
{
  "challengeId": "ma-bi-mat-do-face-tao",
  "actions": ["CENTER", "TURN_RIGHT", "TURN_LEFT", "CENTER"],
  "expiresAt": 1792742400.0
}
```

Mã thử thách là quyền truy cập tạm thời: không chia sẻ hoặc ghi vào log.
Sau khi sai, cần phiên mới từ Core để thử lại. Chỉ retry cùng challenge khi
503 từ Core; kết quả/ảnh cũ được giữ để không nhận dạng lại thành người khác.

## Telegram

Chỉ gửi **ảnh** cho:

- `FACE_LOGIN_FAILED`: đăng nhập khuôn mặt thất bại, kể cả nhận ra mặt nhưng
  Core từ chối tài khoản/phiên tại bước cuối.
- `FACE_ENROLLED`: đăng ký khuôn mặt thành công.

Không gửi ảnh đăng nhập khuôn mặt thành công hoặc đăng ký thất bại. Password
thuộc Core nên service này không gửi thông báo password. Core Java đã có
hàng đợi thông báo chữ cho password đúng/sai và đăng nhập mặt thành công,
xem core-service/docs/TELEGRAM.md. Ảnh theo quy tắc của Face ở trên.
Không có cảnh báo liên tục khi chỉ đứng trước camera.

Ảnh gửi được mã hóa lại JPEG, không gửi nguyên file upload. File hỏng không
giải mã được sẽ không có ảnh để gửi. Sự kiện được lưu vào hàng đợi PostgreSQL
cùng transaction với kết quả; worker gửi riêng, thử lại tối đa 5 lần. Gửi lỗi
không đổi kết quả xác thực. Xem `docs/TELEGRAM.md` về retry, TTL và giới hạn gửi trùng.

## Dữ liệu và giới hạn MVP

- Một hồ sơ cho mỗi `adminId`, lưu vector trung bình từ các frame chính diện.
  Đăng ký lại thay mẫu cũ sau khi thử thách và Core đều chấp nhận.
- Không cho đăng ký một khuôn mặt đã khớp hồ sơ của Admin khác.
- Mỗi frame phải có đúng một mặt; kiểm tra kích thước, độ sáng, tương phản
  và cùng danh tính trong suốt thử thách. Từ chối ảnh mờ theo phương sai
  Laplacian, landmarks không hợp lệ và đầu nghiêng quá 25 độ.
- Liveness dùng vị trí mũi chiếu lên trục hai mắt và thứ tự quay đầu ngẫu nhiên. Đây là
  **MVP thử thách chuyển động**, không phải mô hình chống giả mạo chuyên dụng;
  chưa bảo đảm chống video phát lại/deepfake/camera ảo. Cần thử webcam thực tế
  để hiệu chỉnh ngưỡng góc và chất lượng trước khi dùng ngoài demo.
- Điểm so khớp giữ công thức cosine + khoảng cách của bản cũ; ngưỡng 0.80
  không phải xác suất 80%. Nếu hai hồ sơ có điểm gần nhau dưới 0.05, từ chối.
- Phiên thử thách tồn tại tối đa 90 giây và không lâu hơn phiên Core.
  Tối đa 120 frame, các frame tính tiến độ cách nhau ít nhất 200 ms.
- PostgreSQL khóa hàng của từng thử thách để chặn request đồng thời; đăng ký
  dùng thêm advisory lock để tránh hai Admin đăng ký cùng mặt cùng lúc.
  Inference và callback còn nằm trong transaction; chưa tối ưu cho tải lớn.
- Khi Core tạm lỗi, PostgreSQL giữ ảnh cuối và kết quả chờ retry. Hoàn tất sẽ
  xóa dữ liệu tạm. Chạy lệnh dọn dữ liệu bên dưới định kỳ để xóa dữ liệu của
  phiên hết hạn bị bỏ dở. Hồ sơ embedding không bị xóa bởi lệnh này.
- Core/Gateway cần giới hạn tần suất tạo phiên/thử sai và bảo vệ API nội bộ.
  Face không đọc Core DB. Không đưa API `/internal/**` ra Gateway công khai.

## Kiểm thử

Kiểm thử không tải AI, không bật camera, không gọi Core/Telegram thật.
Các test lưu trữ/nghiệp vụ/API cần PostgreSQL test riêng; nếu thiếu
`TEST_DATABASE_URL`, chúng được đánh dấu SKIP, không được coi là đã đạt:

```powershell
.\.venv-runtime\Scripts\python.exe -m pip install -r requirements-test.txt
$env:TEST_DATABASE_URL = 'postgresql://face_test:MAT_KHAU@localhost:5432/roomix_face_test'
.\.venv-runtime\Scripts\python.exe -m unittest discover -s tests -v
.\.venv-runtime\Scripts\python.exe -m compileall -q src/face_auth tests
```

Dọn thử thách hết hạn, gồm embedding/ảnh tạm:

```powershell
.\.venv-runtime\Scripts\python.exe -m src.face_auth.db.cleanup
```

Kiểm thử client camera (Node.js): `node --test tests/web/face_camera.test.mjs`.
