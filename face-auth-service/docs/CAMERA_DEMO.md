# Thử camera từ trình duyệt

Công cụ tích hợp này không tạo phiên Core, không cấp JWT và không giả lập
quyền Admin. Core đã có hợp đồng trong `CORE_INTEGRATION.md`; cần cấu hình và
chạy Core trước khi lấy phiên thật. Xem core-service/docs/LOCAL_SETUP.md cho
PostgreSQL Windows. Trang quản trị frontend hiện cũng có chức năng camera.

## Bật trang thử

Cấu hình PostgreSQL, Core và token nội bộ như README, sau đó:

```powershell
$env:FACE_ENABLE_DEMO = 'true'
.\.venv-runtime\Scripts\python.exe -m uvicorn src.face_auth.main:app --port 8003 --no-access-log
```

Mở `http://localhost:8003/demo`. Mặc định trang và tài nguyên demo trả 404.
Camera trình duyệt cần localhost hoặc HTTPS. Tắt demo sau khi thử.
`--no-access-log` tránh ghi mã challenge trong URL vào access log mặc định;
không ghi JWT/mã phiên trong log của Gateway hoặc proxy.

## Đăng nhập khuôn mặt

1. Tạo phiên LOGIN bằng API của Core. Phiên phải được ràng buộc với trình
   duyệt; không để người khác lấy JWT chỉ bằng mã phiên.
2. Chọn Đăng nhập khuôn mặt, điền `authSessionId` vừa nhận.
3. Nhấn Bắt đầu, cho phép camera và làm theo hướng dẫn.
4. Khi Face báo thành công, nhận JWT qua API Core của frontend chính.
   Trang demo không tự chuyển vào quản trị hoặc coi kết quả Face là JWT.

## Đăng ký khuôn mặt

1. Đăng nhập Core bằng mật khẩu với tài khoản ADMIN không bị khóa.
2. Dùng JWT tạo phiên ENROLL theo API do người phụ trách Core cung cấp.
3. Chọn Đăng ký khuôn mặt, điền phiên ENROLL và JWT vào trang thử.
4. Hoàn thành thử thách. `adminId` chỉ do Core trả, không nhập trong trang.
   Chỉ báo thành công sau khi hồ sơ được lưu vào PostgreSQL.

JWT không lưu trong localStorage/sessionStorage/cookie/URL. Ô JWT được xóa
khi gửi; token chỉ nằm tạm trong bộ nhớ và chỉ gửi ở request tạo challenge
ENROLL, không gửi lại cùng mỗi ảnh.

## Dừng và lỗi kết nối

- Camera chỉ mở khi nhấn Bắt đầu, không thu âm.
- Từ chối quyền camera không tạo challenge Face.
- Gửi ảnh tuần tự, không có các request nhận dạng chồng nhau.
- Mỗi request có timeout 20 giây. Timeout lúc gửi kết quả sẽ đọc lại trạng
  thái; không tự coi timeout là đăng nhập thất bại.
- Phản hồi HTML hoặc JSON sai cấu trúc sẽ được báo lỗi dịch vụ và tắt camera.
- 503/429 được thử lại tối đa hai lần, giữ đúng ảnh của request cần retry.
- Mất response cuối hoặc `CHALLENGE_ALREADY_USED`: đọc lại trạng thái cũ,
  không xác thực mặt lại hay tạo thêm sự kiện ảnh Telegram. Giới hạn retry
  HTTP Telegram được ghi rõ trong TELEGRAM.md.
- Nút Đọc lại kết quả dùng challenge đang giữ trong bộ nhớ trang.
- Kết thúc, dừng hoặc rời trang sẽ tắt camera track. Dừng giữa chừng để
  phiên hết hạn; chưa coi là kết quả đăng nhập thất bại để gửi ảnh.
- Thử từ đầu cần phiên Core mới. Trang không tự tiếp tục challenge cũ.

API trạng thái: `GET /api/face/challenges/{challengeId}`. Mã challenge là bí
mật ngắn hạn; response chỉ có trạng thái, bước hoặc lý do, không có ảnh,
embedding, `adminId`, `authSessionId` hay JWT. Response dùng no-store.

## File cần đọc

- `src/face_auth/web/index.html`: bố cục trang thử.
- `src/face_auth/web/demo.mjs`: form và hiển thị trạng thái.
- `src/face_auth/web/face_camera.mjs`: client camera có thể dùng lại trong
  frontend chính; frontend Core chịu trách nhiệm tạo phiên/nhận JWT.
- `tests/web/face_camera.test.mjs`: test client bằng camera và HTTP giả.

```powershell
node --test tests/web/face_camera.test.mjs
```

Test JavaScript không cấp quyền camera thật hay xác nhận nhận dạng bằng mô
hình. Vẫn cần thử riêng webcam và Core thật.
