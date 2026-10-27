# Chạy Core trên Windows

Yêu cầu JDK 21, PostgreSQL và cấu hình riêng trong core-service/.env.
Copy .env.example thành .env chỉ khi chưa có file; điền các giá trị thật.
Không gửi mật khẩu/token vào chat và không commit .env.

- CORE_DB_URL: JDBC URL của core_db, không phải database Face.
- CORE_DB_USERNAME/CORE_DB_PASSWORD: role riêng có quyền trên core_db để Flyway
  tạo/migrate bảng. Không cần quyền superuser/tạo database/tạo role khi chạy.
- JWT_SECRET: key Base64 chung của nhóm, ít nhất 32 byte sau giải mã.
- FACE_SERVICE_TOKEN: giống Face Python, ít nhất 32 byte UTF-8.
- KAFKA_BOOTSTRAP_SERVERS: địa chỉ Kafka trên Windows nếu chạy Core local.
- TELEGRAM_BOT_TOKEN/TELEGRAM_CHAT_ID: cùng bot/chat bên Face hoặc cùng để trống.
- CORE_TELEGRAM_WORKER_ENABLED=true khi chủ ý bật gửi thông báo thật.

```powershell
.\scripts\run_local.ps1 -CheckOnly
.\scripts\run_local.ps1
```

CheckOnly chỉ kiểm tra cấu hình/kết nối và bảng, không tạo dữ liệu hay gửi tin.
Database trống có thể báo SCHEMA_PENDING; Flyway V1–V8 chạy khi khởi động Core.
Database đã có bảng mà thiếu lịch sử Flyway phải đối chiếu với nhóm; không tự
baseline hoặc chạy schema.sql cũ. Không còn bootstrap Admin tự động.

Script nạp .env như dữ liệu; biến môi trường sẵn có được ưu tiên. Có thể truyền
-JavaHome/-PostgresDriver khi JDK/driver ở vị trí khác. Nếu chưa có JDBC driver
trong cache Maven, chạy .\mvnw.cmd -DskipTests compile trước.

Core mặc định 8081; cổng local trên máy có thể đổi bằng CORE_PORT. Qua Gateway,
frontend dùng NEXT_PUBLIC_API_URL trỏ Gateway; CORE_ALLOWED_ORIGINS có thể trống
khi CORS do Gateway quản lý. Xem api-gateway/docs/FACE_ADMIN.md từ gốc backend.
Core trong Compose nhận environment trực tiếp; Spring Boot không tự nạp .env.

Xem MAIN_INTEGRATION.md và face-auth-service/docs/LOCAL_ACCEPTANCE.md ở gốc
backend cho hợp đồng hiện tại và kết quả kiểm tra. Không dùng các lệnh init/
bootstrap của bộ khung trước khi tích hợp main.
