# Thông báo ảnh Telegram

Chỉ hai sự kiện được gửi ảnh:

| Sự kiện | Khi tạo |
|---|---|
| `FACE_LOGIN_FAILED` | Core đã phản hồi kết quả đăng nhập mặt thất bại |
| `FACE_ENROLLED` | Core chấp nhận đăng ký và Face lưu hồ sơ thành công |

Không gửi ảnh khi đăng nhập mặt thành công, đăng ký thất bại hoặc dùng mật
khẩu. Core Java đã có thông báo chữ cho đăng nhập mật khẩu đúng/sai và đăng
nhập mặt thành công; xem core-service/docs/TELEGRAM.md. Hai service đọc cấu
hình bot/chat riêng, có thể dùng cùng bot/chat. Không gửi Telegram từ frontend.
Trường hợp không giải mã được file ảnh sẽ không có ảnh để gửi.

## Cấu hình

```powershell
$env:TELEGRAM_BOT_TOKEN = 'TOKEN_BOT_CUA_BAN'
$env:TELEGRAM_CHAT_ID = 'CHAT_ID_NHAN_CANH_BAO'
```

Khi đủ hai biến, worker được chạy cùng ứng dụng. Khi thiếu một trong hai,
không tạo sự kiện ảnh mới hoặc khởi chạy worker gửi. Không đọc token từ
`legacy/cv/security_config.json`; cấu hình bản cũ không áp dụng cho service.

## Hàng đợi PostgreSQL

1. Luồng xác thực ghi sự kiện vào `telegram_outbox` cùng transaction với
   kết quả thử thách và hồ sơ đăng ký (nếu có).
2. Transaction lỗi sẽ rollback cả kết quả lẫn sự kiện, không báo đăng ký
   thành công khi hồ sơ chưa lưu.
3. Worker lấy từng dòng bằng `FOR UPDATE SKIP LOCKED`, gửi ảnh và ghi kết
   quả. Request xác thực không chờ mạng Telegram.
4. Sự kiện chưa gửi vẫn còn sau khi ứng dụng tắt; khởi động lại sẽ gửi tiếp.

Không cần cài thêm RabbitMQ cho chức năng này. `UNIQUE(auth_session_id,
event_type)` ngăn tạo trùng sự kiện nội bộ. Việc đọc lại kết quả hoặc gửi lại
yêu cầu đã hoàn tất không tạo thêm sự kiện.

## Thử lại và lưu ảnh

- Tối đa 5 lần gửi có ghi nhận kết quả. Các lần chờ: 5, 10, 20 và 40 giây.
- Hạn gửi/lưu tạm là 24 giờ. Worker hoặc lệnh cleanup xóa ảnh hết hạn khi
  chạy; nếu cả hai đều dừng, ảnh còn trong database tới lần dọn kế tiếp.
- Gửi thành công: đánh dấu SENT và xóa nội dung cột `photo`.
- Hết 5 lần thử hoặc hết 24 giờ: đánh dấu FAILED, xóa nội dung `photo`.
- Khi worker đang tắt, chạy lệnh cleanup định kỳ để dọn ảnh hết hạn:

```powershell
.\.venv-runtime\Scripts\python.exe -m src.face_auth.db.cleanup
```

Worker không đổi kết quả đăng nhập/đăng ký khi Telegram lỗi. Bot token,
JWT và authSessionId không được đưa vào caption/log. Caption có mã sự kiện
nội bộ, Admin xác định được (nếu có), lý do và thời gian xảy ra sự kiện.

## Giới hạn gửi trùng

Chống tạo trùng trong PostgreSQL không đồng nghĩa Telegram luôn nhận đúng
một tin: nếu Telegram đã nhận ảnh nhưng kết nối đứt trước khi lưu trạng thái
SENT, worker có thể thử lại và gửi hai lần cùng mã sự kiện. Không tuyên bố
bảo đảm exactly-once cho HTTP gọi ra ngoài. Xem API
[sendPhoto của Telegram](https://core.telegram.org/bots/api#sendphoto).

## Tra cứu trạng thái

Chạy trong database Face, không cần lấy ảnh hoặc token:

```sql
SELECT id, event_type, admin_id, status, attempts, created_at, completed_at
FROM telegram_outbox
ORDER BY id DESC
LIMIT 50;
```

## File liên quan

- `services/face_workflow.py`: quyết định tạo sự kiện.
- `db/notification_store.py`: truy vấn hàng đợi và trạng thái retry.
- `services/telegram_worker.py`: chạy gửi nền, tôn trọng shutdown.
- `integrations/telegram_notifier.py`: gọi Telegram sendPhoto.
- `tests/test_telegram_queue.py`: retry, restart, rollback, TTL, hai worker;
  mọi lần gửi đều giả lập, không gửi thông báo thật.
