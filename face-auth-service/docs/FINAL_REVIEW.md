# Rà soát cuối phần Admin/Face — 27/10/2026

## Đã dọn và chuẩn hóa

- Bỏ code điểm danh/dashboard/Maven trong legacy, test Telegram legacy và file
  OpenApiConfig.java còn sót từ bộ khung Java Face. Bản gốc lưu cục bộ tại
  .tmp/review-archive/original-unused-source.zip; không đưa vào Git.
- Bỏ các hàm cache/nhận dạng cũ không được luồng Admin gọi; giữ pipeline AI
  đang dùng và matching.py. Trọng số chỉ tìm trong models/ hoặc thư mục chạy.
- Xóa .venv Python 3.12 không còn dùng sau khi kiểm tra không có tiến trình
  sử dụng. .venv-runtime Python 3.11 là môi trường chạy và kiểm thử duy nhất.
- Cập nhật tài liệu Core theo AuthServiceImpl/JwtTokenProvider của main,
  Flyway V1–V8; bỏ hướng dẫn chạy bootstrap/schema.sql cũ.
- Cập nhật kết quả webcam/Telegram/Docker trong tài liệu Face/Core/frontend.
- Bổ sung .env.example ở gốc backend, để trống secret; bỏ mẫu Docker Face trùng.
- Củng cố ignore cho file tạm, trọng số, embedding, database export và log.
  Thêm scripts/audit_git.py để quét working tree, file mới và Git index hai repo.
- PostCSS giữ cấu hình cũ, chỉ đặt tên biến export để bỏ cảnh báo ESLint.

## Kết quả kiểm tra lại

| Kiểm tra | Kết quả |
|---|---|
| Python Face + schema PostgreSQL riêng cho từng test | 118 đạt, không skip |
| Frontend API/camera | 44 đạt, không skip |
| TypeScript frontend | Đạt |
| ESLint file Admin/Face/cấu hình thuộc phần này | Đạt |
| AI CPU thật sau dọn source | Tải model đạt, embedding 512 chiều, từ chối ảnh trống |
| Compose dùng cấu hình mẫu + secret ngẫu nhiên riêng | Đạt |
| git diff --check và cached --check | Đạt |

Số test Face trước đây là 125; giảm 7 do bỏ test của Telegram legacy, không bỏ
test của pipeline hiện tại. Test Telegram dùng mock, không gửi thông báo thật.
Unit test dùng schema ngẫu nhiên và dọn đúng schema đó; không sửa hồ sơ thật.
Các kết quả Core 57 test và Gateway 20 test từ lần tích hợp trước được giữ làm
mốc; lần dọn này không sửa logic Java nên không chạy lại toàn bộ hai bộ đó.

## Git và dữ liệu riêng

Quét các file tracked/new và Git index hiện tại: không phát hiện giá trị secret
đang cấu hình hoặc các mẫu bot token/private key được kiểm tra. .env/.env.local/
.env.production, .tmp, venv và dependency được bỏ qua. File mẫu được giữ trong Git.
Backend không có file ảnh/video/embedding/database trong danh sách hiện tại.
12 ảnh frontend đã xem là logo, linh vật và phòng mẫu, không có ảnh đăng ký mặt.
Fixture embedding trong test là dữ liệu tổng hợp, không phải hồ sơ người dùng.

Đây là quét theo giá trị và định dạng đã biết, không phải bảo đảm phát hiện mọi
loại secret. Không dùng git add -f với file riêng. Không commit database/ảnh thật.
Danh sách file thay đổi: CHANGED_FILES.md; báo cáo máy đọc được ở
.tmp/final-git-review.json và .tmp/final-history-review.json, không đưa vào Git.

**Còn phải phối hợp:** JWT_SECRET hiện đang dùng trùng giá trị trong lịch sử Git
của Core/Rental. Tìm thấy 8 phiên bản blob lịch sử có giá trị đã biết; không có
nghĩa là 8 secret khác nhau. Cấu hình hiện tại đã đọc JWT từ environment, nhưng
việc này không thu hồi key cũ. Cần người phụ trách Core thay key chung và cập
nhật các service dùng JWT. Không tự đổi key của nhóm hoặc viết lại Git history.
Quét lịch sử này không phát hiện .env, bot token dạng chuẩn, private key hoặc
file database/embedding theo các định dạng kiểm tra. Frontend không có phát hiện
trong cùng phạm vi quét lịch sử. Các ảnh đã gửi vào chat vẫn là nguồn lộ riêng,
không được xử lý bằng việc dọn Git.

Rental application.properties đã được stage riêng để bỏ key ghi sẵn khỏi index.
Không commit, push, force-push hoặc xóa lịch sử. Frontend là repo riêng.

## Lỗi frontend ngoài phạm vi Face còn tồn tại

Chạy npm run lint toàn frontend còn 7 lỗi ở các file không thuộc thay đổi Face:

| File | Lỗi |
|---|---|
| src/app/admin/users/page.tsx | set-state-in-effect |
| src/components/layouts/FilterModal.tsx | set-state-in-effect |
| src/components/layouts/RejectModal.tsx | set-state-in-effect |
| src/components/ui/Charts.tsx | immutability |
| src/lib/admin-store.ts | set-state-in-effect |
| src/lib/landlord-store.ts | set-state-in-effect |
| src/lib/tenant-store.ts | no-empty-object-type |

Không tắt rule lint hoặc sửa nghiệp vụ các thành viên khác để che các lỗi này.
Không kết luận toàn frontend đã đạt lint. Build frontend đã đạt ở lần sửa CSS
trước; lần này xác nhận lại TypeScript và lint theo phạm vi, không chạy lại build.

## Giữ lại có chủ đích

.env, models/, .venv-runtime, cấu hình/credential/state thử cục bộ trong .tmp,
database và Docker volume được giữ để tiếp tục chạy. Không dùng down -v, không
xóa hồ sơ mặt. Các file .tmp không phải thành phần bàn giao hay image runtime.
Nghiệp vụ Rental/RAG, các trang mock và benchmark chống giả mạo nằm ngoài lần
rà soát Face/Admin này. Xem LOCAL_ACCEPTANCE.md để biết giới hạn kiểm chứng.
