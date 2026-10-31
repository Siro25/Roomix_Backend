# Tra cứu mã nguồn

Mã đang chạy nằm trong `src/face_auth/`. Các file được di chuyển từ backend
ban đầu, không tạo một bản Face Auth thứ hai. Chạy mọi lệnh từ gốc dự án.

| Muốn sửa phần nào? | File cần mở |
|---|---|
| Khởi tạo ứng dụng | `src/face_auth/main.py` |
| URL và xử lý HTTP | `src/face_auth/api/routes.py` |
| Kiểu dữ liệu API | `src/face_auth/api/schemas.py` |
| YOLO, MTCNN, FaceNet | `src/face_auth/ai/face_service.py` |
| Ảnh đầu vào | `src/face_auth/ai/image_input.py` |
| Hướng quay đầu | `src/face_auth/ai/head_pose.py` |
| Thử thách người thật | `src/face_auth/ai/liveness.py` |
| Điểm so khớp | `src/face_auth/ai/matching.py` |
| Luồng đăng nhập và đăng ký | `src/face_auth/services/face_workflow.py` |
| Lưu hồ sơ và thử thách | `src/face_auth/db/storage.py` |
| Dọn dữ liệu tạm | `src/face_auth/db/cleanup.py` |
| Giao tiếp Core Java | `src/face_auth/integrations/core_client.py` |
| Hàng đợi Telegram | `src/face_auth/db/notification_store.py`, `services/telegram_worker.py`, `docs/TELEGRAM.md` |
| Gửi ảnh Telegram | `src/face_auth/integrations/telegram_notifier.py` |
| Trang thử camera | `src/face_auth/web/`, `docs/CAMERA_DEMO.md` |
| Cài/chẩn đoán môi trường | `scripts/setup_runtime.ps1`, `src/face_auth/check_runtime.py`, `docs/RUNTIME.md` |
| Cấu hình | `src/face_auth/config.py`, `.env.example` |
| Docker Python và Compose | `Dockerfile`, `docs/DOCKER.md`, `../compose.yml` |
| Tiến độ tích hợp và kiểm tra trên máy thật | `docs/LOCAL_ACCEPTANCE.md` |

## Đường dẫn cũ và mới

| Trước đây | Hiện tại |
|---|---|
| `src/web/backend/face_service.py` | `src/face_auth/ai/face_service.py` |
| `src/web/backend/main.py` | `src/face_auth/main.py` và `api/routes.py` |
| `src/web/backend/workflow.py` | `src/face_auth/services/face_workflow.py` |
| `src/web/backend/storage.py` | `src/face_auth/db/storage.py` |

Code điểm danh, dashboard cũ và bộ khung Maven đã được bỏ khỏi cây source.
Bản gốc lưu cục bộ trong `.tmp/review-archive/original-unused-source.zip`,
không chạy cùng ứng dụng và không đưa vào Git. Pipeline AI hiện tại vẫn được
phát triển từ code ban đầu. Trọng số được tìm trong `models/` rồi thư mục chạy.

## Đọc code theo thứ tự

1. `api/routes.py`: yêu cầu đi vào đâu.
2. `services/face_workflow.py`: các bước xử lý.
3. `ai/`: phân tích ảnh và so khớp.
4. `db/` và `integrations/`: lưu dữ liệu, gọi dịch vụ ngoài.

PostgreSQL được triển khai trong `db/storage.py`, schema ở `db/schema.sql`.
Xem `DATABASE.md` để cấu hình. Core/Gateway/Face đã được kiểm tra liên thông;
người dùng xác nhận webcam thật và Telegram. Xem `LOCAL_ACCEPTANCE.md` cho
giới hạn kiểm chứng, `HANDOFF.md` để bàn giao và `FINAL_REVIEW.md` cho rà soát Git.
