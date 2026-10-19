# Chạy toàn bộ Roomix Backend bằng Docker Desktop

Compose chạy 5 service ứng dụng (Gateway, Core, Rental, RAG, Face), 2 PostgreSQL và Kafka. Đây là môi trường phát triển trên máy cá nhân. Database trong Docker bắt đầu rỗng, không tự lấy dữ liệu từ PostgreSQL đang cài trên Windows.

## 1 Chuẩn bị

Mở Docker Desktop và đợi trạng thái Engine running. Trước lần build đầu, kiểm tra ổ đang chứa Docker Desktop còn vài GB trống; Maven và Python sẽ tải khá nhiều dependency. Nếu Docker lưu disk image trên ổ C đã gần đầy, chuyển vị trí trong Docker Desktop ở **Settings → Resources → Advanced → Disk image location** sang ổ còn chỗ. Dùng nút trong Docker Desktop, không tự kéo file VHDX bằng Explorer.

Trước khi thử nghiệp vụ thuê qua Docker, dừng các bản Core/Rental chạy bằng Spring Dashboard trên Windows. Chúng đang có thể kết nối cùng Kafka `localhost:9092` và dùng chung consumer group `core-room-projection`; nếu chạy song song, một số event sẽ bị bản Windows nhận thay cho Core trong Docker, làm hai database lệch trạng thái. Việc đổi cổng HTTP không giải quyết được xung đột consumer group này.

Mở PowerShell tại thư mục gốc của repo:

```powershell
cd D:\javaSpring\Roomix\Roomix_Backend
Copy-Item .env.example .env
```

Mở `.env` và đổi ít nhất `ROOMIX_DB_PASSWORD` cùng `ROOMIX_JWT_SECRET`. Hai giá trị mẫu chỉ dùng để thử trên máy cá nhân, không dùng khi đưa lên server. Có thể tạo JWT secret mới bằng PowerShell:

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Dán kết quả vào `ROOMIX_JWT_SECRET`. Để `GEMINI_API_KEY` trống thì RAG vẫn chạy ở chế độ mock; muốn gọi Gemini thật thì điền API key của bạn vào `.env`. Tương tự, chức năng upload ảnh phòng cần ba biến `CLOUDINARY_*`; các API khác vẫn khởi động khi chúng để trống. File `.env` đã được Git bỏ qua, không commit nó.

## 2 Khởi động

```powershell
docker compose config --quiet
docker compose --parallel 1 build
docker compose up -d --no-build
docker compose ps
```

Lần đầu Docker cần tải image nền và cài dependency nên có thể mất vài phút. `--parallel 1` build từng image để tránh hết RAM trên Docker Desktop. `-d` để container chạy nền. Những lần sau, nếu không đổi code, chỉ cần `docker compose up -d`.

Compose dùng lại cấu hình Kafka trong `compose.kafka.yml`. Nếu Kafka của repo đã chạy từ trước, Compose có thể cập nhật container đó nhưng giữ volume dữ liệu Kafka.

## 3 Kiểm tra từng service

| Thành phần | Địa chỉ trên Windows | Địa chỉ giữa các container |
| --- | --- | --- |
| Gateway và Swagger | http://localhost:18080/swagger-ui/index.html | api-gateway:8080 |
| Core OpenAPI | http://localhost:18081/v3/api-docs | core-service:8081 |
| Rental OpenAPI | http://localhost:18082/v3/api-docs | rental-service:8082 |
| RAG Swagger | http://localhost:18083/docs | rag-service:8083 |
| Face OpenAPI | http://localhost:18084/v3/api-docs | face-auth-service:8084 |
| Core PostgreSQL | localhost:15432, database `core_db` | core-db:5432 |
| Rental PostgreSQL | localhost:15433, database `rental_db` | rental-db:5432 |
| Kafka | localhost:9092 | kafka:19092 |

Các cổng 18080–18084 tránh đụng service đang chạy trên Windows và dự án Docker khác có thể dùng 8080. Gateway Docker ở cổng host 18080 trỏ vào các container, không trỏ vào tiến trình Java trên Windows. Nếu 18080 đã bị chiếm, đổi `GATEWAY_HOST_PORT` trong `.env`. Các container vẫn nói chuyện với Gateway qua cổng nội bộ 8080.

Thử các lệnh:

```powershell
Invoke-RestMethod http://localhost:18081/v3/api-docs | Select-Object openapi
Invoke-RestMethod http://localhost:18082/v3/api-docs | Select-Object openapi
Invoke-RestMethod http://localhost:18083/health
docker compose exec core-db psql -U postgres -d core_db -c "SELECT version FROM flyway_schema_history ORDER BY installed_rank;"
docker compose exec rental-db psql -U postgres -d rental_db -c "SELECT version FROM flyway_schema_history ORDER BY installed_rank;"
```

Gateway chuyển `/api/ai/chat` sang `/api/v1/chat` của RAG. Trang Swagger ở Gateway tổng hợp Gateway, Core và Rental; RAG có trang `/docs` riêng. Face hiện là service khung, chạy được nhưng chưa có API nhận diện khuôn mặt hoàn chỉnh.

## 4 Kafka hoạt động như thế nào

Kafka là nơi chuyển sự kiện giữa Rental và Core; nó không thay PostgreSQL. Khi Chủ trọ xác nhận cọc hoặc kết thúc hợp đồng, Rental lưu thay đổi hợp đồng và một hàng vào bảng `tenancy_events` trong cùng giao dịch. Hàng này gọi là outbox: nếu transaction thất bại thì cả hợp đồng và sự kiện đều không được lưu.

Sau commit, `TenancyOutboxScheduler` đọc hàng chưa phát và `TenancyOutboxPublisher` gửi JSON vào topic `rental-events-topic`. Rental đặt `roomId` làm Kafka key. Topic có 3 partition; cùng một key luôn vào cùng partition, nên sự kiện của cùng một phòng giữ đúng thứ tự. Khi Kafka xác nhận, Rental ghi `published_at`; nếu gửi lỗi, `attempt_count` và `next_attempt_at` dùng để thử lại.

Core dùng consumer group `core-room-projection` để nhận sự kiện. Với `TENANCY_ACTIVATED`, Core đặt phòng `OCCUPIED` và `is_rented=true`. Với `TENANCY_ENDED`, Core đưa phòng đang `OCCUPIED` về `AVAILABLE` và `is_rented=false`. Core ghi `eventId` và `sequence` vào `processed_rental_events`, rồi mới xác nhận offset Kafka. Vì vậy bản tin đến lại sẽ không làm cập nhật trùng hoặc đảo trạng thái cũ.

```text
Rental DB (hợp đồng + outbox)
          ↓ publisher
Kafka topic rental-events-topic (key = roomId)
          ↓ consumer
Core DB (ROOM + processed_rental_events)
```

Trong Docker, `localhost` bên trong Core là chính container Core, không phải Kafka. Vì thế Core/Rental dùng `kafka:19092`; công cụ trên Windows dùng `localhost:9092`. Tương tự, Rental gọi Core qua `http://core-service:8081`.

## 5 Xem log và xử lý lỗi thường gặp

```powershell
docker compose ps
docker compose logs --tail=100 core-service rental-service
docker compose logs --tail=100 kafka
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:19092 --describe --topic rental-events-topic
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka:19092 --describe --group core-room-projection
```

- Nếu Core/Rental chưa lên, xem log Flyway và kiểm tra `ROOMIX_DB_PASSWORD`. Password trong `.env` chỉ được PostgreSQL dùng để tạo database khi volume còn rỗng; đổi password sau đó không tự đổi user trong database cũ.
- Nếu cổng 8080 hoặc 9092 bị chiếm bởi ứng dụng khác, dừng ứng dụng đó hoặc đổi cổng host. Đổi cổng 9092 cần sửa cả cấu hình Kafka listener dành cho host.
- Nếu RAG không gọi được Gemini, kiểm tra `GEMINI_API_KEY`. Không có key, service chạy mock mode.
- Nếu consumer gặp bản tin lỗi không thể xử lý, nó sẽ retry liên tục thay vì bỏ qua. Xem log Core và sửa nguyên nhân trước khi chờ các sự kiện sau của cùng partition.

## 6 Dừng và giữ dữ liệu

```powershell
docker compose down
```

Lệnh này dừng/xóa container nhưng giữ named volumes của Core DB, Rental DB, Kafka và ChromaDB. `docker compose down -v` sẽ xóa các volume đó và làm mất dữ liệu môi trường Docker; không chạy lệnh này nếu chưa sao lưu.

Khi cần build lại sau khi sửa code:

```powershell
docker compose --parallel 1 build
docker compose up -d --no-build
```

Không cần đăng nhập Docker Hub hoặc GHCR để build ứng dụng từ code local. Docker vẫn cần tải các image nền công khai ở lần đầu; GHCR chỉ cần khi bạn muốn kéo image đã được CI đẩy lên registry.
