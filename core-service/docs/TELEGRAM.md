> Sau khi tích hợp main: xem [MAIN_INTEGRATION.md](MAIN_INTEGRATION.md).
> Các lệnh bootstrap Admin, CORE_DB_INIT_MODE và cấu hình JWT cũ bên dưới không còn áp dụng.

# Thông báo đăng nhập Admin qua Telegram

## Phân công Core và Face

| Sự kiện | Service gửi | Loại |
|---|---|---|
| Mật khẩu đúng | Core | Chữ |
| Mật khẩu sai / tài khoản không được phép đăng nhập Admin | Core | Chữ |
| Đăng nhập khuôn mặt thành công và Core cấp JWT | Core | Chữ |
| Đăng nhập khuôn mặt thất bại có ảnh hợp lệ | Face Python | Ảnh |
| Đăng ký khuôn mặt thành công | Face Python | Ảnh |

Core không gửi ảnh và không tạo thêm thông báo chữ cho callback Face thất
bại hoặc đăng ký mặt. Ảnh theo quy tắc của Face. File hỏng không giải mã
được thì Face không có ảnh để gửi. Body đăng nhập không hợp lệ (HTTP 400)
khác với kiểm tra thông tin không đạt (401); chỉ lần kiểm tra mật khẩu 401
được ghi nhận thất bại. Lỗi hạ tầng 503 không được coi là sai mật khẩu.

## Cấu hình

Thêm vào environment Core qua terminal/IDE/Compose:

```dotenv
TELEGRAM_BOT_TOKEN=token_bot_cua_ban
TELEGRAM_CHAT_ID=chat_id_cua_ban
CORE_TELEGRAM_WORKER_ENABLED=true
```

Có thể dùng cùng bot/chat đang cấu hình bên Face. Hai service đọc environment
riêng; Core không tự đọc .env của Face. Cả hai giá trị trống sẽ tắt gửi và
không giữ thông báo chờ trong DB. Điền thiếu một giá trị sẽ dừng startup để
báo cấu hình chưa đầy đủ. Không đưa bot token vào frontend hoặc Git.

Database core_db cần bảng `auth_telegram_outbox` từ
`src/main/resources/telegram-outbox.sql`. Với DB đã có, chạy riêng SQL này
qua Query Tool trước khi bật bot, hoặc bật CORE_DB_INIT_MODE=always một lần
sau khi đối chiếu schema rồi tắt về never. File SQL chỉ CREATE IF NOT EXISTS,
không xóa users hoặc bảng phiên Face. Chặng phát triển không chạy SQL trên
database của người dùng.

CORE_TELEGRAM_WORKER_ENABLED=false dừng worker nhưng vẫn ghi hàng đợi nếu
đã có đủ bot/chat. Chỉ dùng khi bảo trì hoặc kiểm thử; muốn tắt hoàn toàn thì
để trống cả bot/chat. Khởi động lại Core sau khi đổi cấu hình.

## Hàng đợi và retry

- Request đăng nhập chỉ ghi PostgreSQL, không chờ HTTP Telegram.
- Sự kiện và cấp JWT dùng chung transaction. Riêng AuthFailure 401 cho phép
  commit sự kiện thất bại, không rollback nó khi trả lỗi đăng nhập.
- FACE_LOGIN_SUCCEEDED chỉ ghi ở lần đầu complete cấp JWT. Retry complete
  không tạo thêm sự kiện; bảng còn có event_key UNIQUE để chặn ghi trùng.
- Mỗi lần gọi đăng nhập mật khẩu là một lần thử riêng. Frontend không tự
  retry mật khẩu; người dùng thử lại sẽ có sự kiện mới.
- Worker chạy mỗi giây, lấy một dòng bằng FOR UPDATE SKIP LOCKED. Khi gửi,
  worker giữ khóa dòng; worker khác không gửi cùng dòng đang xử lý.
- Tối đa 5 lần thử, thời gian chờ 5, 10, 20, 40 giây. HTTP 429 dùng thời gian
  chờ không nhỏ hơn retry_after của Telegram. Nếu không thể chờ trong TTL
  24 giờ thì chuyển FAILED và xóa nội dung, không gửi sớm hơn yêu cầu API.
- SENT/FAILED hoặc hết TTL sẽ xóa message khỏi DB. Nếu worker dừng, nội dung
  hết hạn chỉ được xóa khi worker chạy lại hoặc được dọn thủ công.
- Worker gửi lỗi không thay đổi JWT/kết quả đăng nhập đã hoàn tất. Lỗi ghi
  hàng đợi do database/migration vẫn là lỗi database, không phải lỗi Telegram.

Nếu Telegram đã nhận tin nhưng mất response hoặc DB chưa ghi SENT, retry
có thể gửi trùng. Không bảo đảm exactly-once; mã sự kiện trong caption giúp
đối chiếu. Không lưu password, JWT, completionToken hoặc ảnh trong hàng đợi.
Caption chứa thời gian UTC, mã sự kiện, username và UUID Admin khi đã xác
thực. Username ở lần sai được ghi là “Tài khoản nhập”, chưa phải danh tính
đã xác nhận; ký tự xuống dòng/định dạng điều khiển bị loại bỏ.

## Dọn dữ liệu khi cần

Trong đúng core_db:

```sql
UPDATE auth_telegram_outbox
SET status = 'FAILED', message = NULL, completed_at = CURRENT_TIMESTAMP
WHERE status = 'PENDING' AND expires_at <= CURRENT_TIMESTAMP;

DELETE FROM auth_telegram_outbox
WHERE status <> 'PENDING' AND completed_at < CURRENT_TIMESTAMP - INTERVAL '7 days';
```

Không có lịch dọn bên ngoài được đăng ký tự động trong chặng này.

## Kiểm thử và giới hạn kiểm chứng

Kiểm thử dùng PostgreSQL/schema riêng như ADMIN_AUTH.md, HTTP Telegram được
mock. Có kiểm tra worker schedule bật/tắt, sai mật khẩu vẫn giữ sự kiện,
retry JWT không nhân bản thông báo, HTTP lỗi/JSON sai, 429, hết TTL, restart
worker và hai worker đồng thời. Không gọi bot thật hoặc gửi tin nhắn thật.

Tổng 63 kiểm thử Core đạt, không SKIP (gồm 42 kiểm thử xác thực/Face trước
đó và 21 kiểm thử thông báo/scheduler mới). Database test đã được dọn sau chạy.

API sử dụng: [sendMessage](https://core.telegram.org/bots/api#sendmessage)
và [ResponseParameters.retry_after](https://core.telegram.org/bots/api#responseparameters).
