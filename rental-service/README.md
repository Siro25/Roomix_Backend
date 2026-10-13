# Rental Service

Nghiệp vụ yêu cầu thuê, hợp đồng và hóa đơn. Package gốc: `com.team4.rental`.
Code sử dụng Lombok và builder, xử lý trực tiếp trong từng phương thức service,
không thêm lớp helper/utility hoặc hàm mapper dùng chung.

## Phạm vi hiện tại

- Yêu cầu thuê: tạo, xem danh sách của mình, chủ trọ chấp nhận hoặc từ chối.
- Chấp nhận tạo hợp đồng PENDING và từ chối mọi yêu cầu PENDING khác của cùng phòng trong một transaction.
- Hợp đồng: PENDING → ACTIVE khi chủ trọ xác nhận cọc; PENDING → CANCELLED;
  ACTIVE → ENDED khi chủ trọ xác nhận trả phòng. Không tự kết thúc chỉ vì ngày trên lịch đã qua:
  cần xác nhận bàn giao thực tế qua API end.
- Hóa đơn: PENDING → CONFIRMED bởi khách thuê → PAID bởi chủ trọ.
- Khiếu nại: khách thuê đánh dấu disputed và ghi lý do; chủ trọ giải quyết kèm nội dung,
  sau đó khách phải xác nhận lại.
- Đã bỏ AMQP/RabbitMQ dependency khỏi Core và Rental.
- Chưa có Kafka producer, consumer, broker, scheduler hay Kafka dependency.

## Đối chiếu note database

Yêu cầu mới và rental structure có một số điểm khác tài liệu database cũ:

| Nội dung | Ánh xạ trong triển khai |
| --- | --- |
| INTEREST_REQUEST | rental_requests, giữ post_id và bổ sung room_id, landlord_id để kiểm tra quyền |
| CONTRACT | Entity Tenancy → bảng contracts; trạng thái PENDING, ACTIVE, ENDED, CANCELLED theo yêu cầu mới |
| ACTIVE_TENANCY | active_tenancies chỉ giữ phiên đang thuê; room_id và contract_id đều UNIQUE |
| METER_READING | meter_readings có contract_id; UNIQUE(contract_id, billing_month), không nối số theo room_id |
| INVOICE | invoices có contract_id, meter_reading_id, PENDING/CONFIRMED/PAID và is_disputed |
| INVOICE_ITEM | invoice_items tách rõ tiền phòng, tiền điện, tiền nước |
| Khóa theo phòng | room_booking_locks đồng bộ transaction kể cả khi chưa có hợp đồng |
| Sự kiện chờ gửi | tenancy_events lưu sự kiện cùng transaction, chưa phát ra Kafka |

UUID user/room/post là tham chiếu sang Core, không tạo foreign key xuyên hai database.
Foreign key giữa các bảng trong rental_db vẫn được tạo đầy đủ.
Lịch sử hợp đồng và hóa đơn được giữ lại; kết thúc thuê chỉ xóa bản ghi active_tenancies.

## Chạy local

Tạo database rỗng `rental_db` trong PostgreSQL. Flyway chạy
`V1__create_rental_business.sql` ở lần khởi động đầu và Hibernate chỉ validate schema.
Không sửa migration sau khi đã áp dụng; thêm migration V2 nếu cần thay đổi tiếp.

Tại thư mục rental-service, cấu hình trong PowerShell:

```powershell
$env:RENTAL_DB_URL = "jdbc:postgresql://localhost:5432/rental_db"
$env:RENTAL_DB_USERNAME = "postgres"
$env:RENTAL_DB_PASSWORD = "<mat-khau-postgresql>"
$env:JWT_SECRET = "<cung-gia-tri-base64-dang-dung-o-Core>"
$env:JWT_ISSUER = "roomix-core"
$env:CORE_SERVICE_URL = "http://localhost:8081"
.\mvnw.cmd spring-boot:run
```

JWT_SECRET bắt buộc chứa tối thiểu 32 byte sau giải mã Base64 và giống Core Service.
JWT kiểm tra HS256, issuer, audience roomix-api, expiration, issued-at, UUID subject và role.
Rental lắng nghe 8082. Gateway 8080 đã có route /api/rentals/**, /api/tenancies/**, /api/invoices/**.
Các API nội bộ Core không được route qua Gateway.

Mỗi request đã xác thực đều gọi Core để xác nhận tài khoản còn ACTIVE và role chưa thay đổi.
Core không phản hồi hoặc trả dữ liệu không hợp lệ thì Rental từ chối thao tác (503);
tài khoản bị khóa/token không hợp lệ trả 401. HTTP client có timeout 3 giây kết nối, 5 giây đọc.

## REST API

Gửi `Authorization: Bearer <accessToken>`. userId luôn lấy từ JWT.
Không có endpoint nhận landlordId/tenantId từ client để quyết định chủ sở hữu.
Danh sách hỗ trợ `page=0&size=20`, size từ 1 đến 100.
Response được bọc trong ApiResponse với code, message, data.
Các entity trả về chỉ gồm dữ liệu scalar, không có quan hệ JPA lazy.

| Method | Endpoint | Quyền và hành vi |
| --- | --- | --- |
| POST | /api/rentals | TENANT gửi yêu cầu; trả 201 |
| GET | /api/rentals | TENANT xem yêu cầu mình gửi; LANDLORD xem yêu cầu gửi đến mình |
| POST | /api/rentals/{requestId}/accept | LANDLORD sở hữu phòng; trả hợp đồng vừa tạo |
| POST | /api/rentals/{requestId}/reject | LANDLORD sở hữu phòng; reason bắt buộc |
| GET | /api/tenancies | Hợp đồng thuộc người gọi |
| GET | /api/tenancies/{tenancyId} | Chỉ hai bên hợp đồng |
| POST | /api/tenancies/{tenancyId}/confirm-deposit | LANDLORD xác nhận đã nhận đủ cọc theo hợp đồng |
| POST | /api/tenancies/{tenancyId}/cancel | Hai bên hợp đồng; chỉ PENDING, reason bắt buộc |
| POST | /api/tenancies/{tenancyId}/end | LANDLORD; chỉ ACTIVE, actualReturnDate bắt buộc |
| POST | /api/invoices | LANDLORD; hợp đồng ACTIVE, trả 201 |
| GET | /api/invoices | Hóa đơn thuộc người gọi |
| GET | /api/invoices/{invoiceId} | Chi tiết hóa đơn, chỉ số và ba dòng phí |
| POST | /api/invoices/{invoiceId}/confirm | TENANT của hợp đồng |
| POST | /api/invoices/{invoiceId}/pay | LANDLORD; chỉ CONFIRMED và không khiếu nại |
| POST | /api/invoices/{invoiceId}/dispute | TENANT; chưa PAID, reason bắt buộc |
| POST | /api/invoices/{invoiceId}/resolve-dispute | LANDLORD; reason là nội dung giải quyết |

Không thuộc quyền sở hữu trả 404. Sai vai trò trả 403. Sai trạng thái hoặc đặt trùng trả 409.
Xác nhận cọc, kết thúc, hủy và thu tiền lặp lại trả trạng thái hiện có, không tạo thêm sự kiện/thu tiền.

### Gửi yêu cầu thuê

```json
{"roomId":"<UUID-phong>","note":"Tôi muốn xem và thuê phòng"}
```

Rental gọi `GET /internal/rental/rooms/{roomId}` của Core bằng JWT người gọi.
Kiểm tra POST APPROVED, ROOM AVAILABLE, isRented false; Rental tự kiểm tra hợp đồng
PENDING/ACTIVE và active_tenancies. Chấp nhận kiểm tra lại phòng, chủ sở hữu và bài đăng.

### Chấp nhận yêu cầu

```json
{
  "startDate":"2026-10-11",
  "endDate":"2027-10-11",
  "monthlyRent":3000000,
  "depositAmount":3000000,
  "electricityUnitPrice":3500,
  "waterUnitPrice":15000,
  "initialElectricity":100,
  "initialWater":20,
  "termsAndConditions":"Thanh toán vào đầu mỗi tháng"
}
```

Thay ngày mẫu cho phù hợp: startDate không trước hôm nay hoặc availableFrom của phòng,
endDate phải sau startDate. Chủ trọ cung cấp các mức giá đã thỏa thuận và số công tơ đầu kỳ
của hợp đồng. Tiền phòng > 0; cọc, đơn giá và chỉ số >= 0, tối đa hai chữ số thập phân.

### Tạo hóa đơn

```json
{
  "tenancyId":"<UUID-hop-dong>",
  "billingMonth":"2026-10",
  "readingDate":"2026-10-11",
  "dueDate":"2026-10-18",
  "oldElectricityVal":100,
  "newElectricityVal":110,
  "oldWaterVal":20,
  "newWaterVal":22
}
```

- Kỳ đầu là tháng startDate; kỳ sau phải bằng kỳ trước + 1 tháng.
- Số cũ kỳ đầu bằng initialElectricity/initialWater của hợp đồng; kỳ sau bằng số mới kỳ trước.
- Số mới >= số cũ. Hợp đồng khác của cùng phòng có chuỗi chỉ số độc lập.
- readingDate phải thuộc billingMonth và thời gian hợp đồng, không nằm trong tương lai.
  dueDate không trước readingDate.
- Tính bằng BigDecimal, làm tròn từng dòng tiền đến hai số thập phân bằng HALF_UP.
  Ví dụ trên: 3.000.000 + 10 × 3.500 + 2 × 15.000 = 3.065.000.
- Tiền phòng hiện tính nguyên tháng theo monthlyRent, chưa chia theo số ngày thực ở.
  Phí dịch vụ bổ sung chưa thuộc phạm vi API này.
- Thu tiền: `{"paymentMethod":"CASH"}` hoặc `{"paymentMethod":"BANK_TRANSFER"}`.
  Đây là chủ trọ ghi nhận đã thu đủ tiền, không phải tích hợp cổng thanh toán.
- Khiếu nại/từ chối/hủy: `{"reason":"Nội dung"}`.
  Trả phòng: `{"actualReturnDate":"2026-10-11"}`.
  Chỉ số/hóa đơn đã phát hành không được sửa qua API; giải quyết khiếu nại ghi nội dung đối soát,
  không tự thay tiền hoặc tự đánh dấu PAID.

## Chống tranh chấp đồng thời

1. Khóa hàng room_booking_locks theo roomId trước khi tạo/duyệt/từ chối yêu cầu hoặc chuyển trạng thái thuê.
2. PostgreSQL partial UNIQUE trên contracts(room_id) với status IN ('PENDING','ACTIVE')
   chặn hai hợp đồng đang giữ cùng phòng, kể cả lúc chưa nhận cọc.
3. active_tenancies.room_id UNIQUE là chốt cuối khi kích hoạt thuê.
4. Hóa đơn khóa hàng hợp đồng; kết thúc hợp đồng cũng khóa cùng hàng.
   Hai lần tạo cùng kỳ không thể tạo hai bản ghi hoặc ghi lệch chuỗi công tơ.
5. Hóa đơn có UNIQUE(contract_id, billing_month); cập nhật xác nhận, thu tiền và khiếu nại khóa hàng invoice.

## Giai đoạn Kafka sau này

Kích hoạt và kết thúc lưu TenancyEvent trong bảng tenancy_events cùng transaction với hợp đồng.
Nếu transaction rollback, bản ghi sự kiện cũng rollback. Mỗi sự kiện có eventId riêng,
sequence, roomId, tenancyId, eventType, topic rental-events-topic và partitionKey = roomId.
Chọn roomId để cả sự kiện của hai hợp đồng kế tiếp trên cùng phòng có cùng key.

**Hiện chưa đồng bộ isRented ở Core.** Search bên Core có thể vẫn thấy phòng đã có hợp đồng,
nhưng Rental luôn chặn yêu cầu/accept dựa trên database của mình.
published_at luôn null cho đến khi triển khai publisher thực.

Khi bổ sung Kafka: đọc outbox theo thứ tự sequence của từng roomId, gửi cùng partition key,
chỉ đánh dấu published_at sau broker acknowledgment; consumer Core dùng eventId chống xử lý lặp.
Partition key đảm bảo thứ tự trong partition sau khi gửi, không tự sửa được thứ tự sai từ nhiều publisher:
publisher cần khóa/điều phối để không vượt sự kiện trước của cùng roomId.

## Kiểm thử

Unit test JWT và Core HTTP client không cần database:

```powershell
.\mvnw.cmd clean verify
```

Integration test cần PostgreSQL, tự tạo schema rental_test_<UUID> biệt lập, áp dụng migration
và xóa đúng schema này sau test. User test cần quyền CREATE SCHEMA. Không dùng schema chứa dữ liệu thật.

```powershell
$env:RENTAL_TEST_DB_URL = "jdbc:postgresql://localhost:5432/postgres"
$env:RENTAL_TEST_DB_USERNAME = "postgres"
$env:RENTAL_TEST_DB_PASSWORD = "<mat-khau-test>"
.\mvnw.cmd clean verify
```

Nếu không có RENTAL_TEST_DB_URL, integration test được báo SKIPPED chứ không coi như đã kiểm chứng database.
Các ca gồm accept/đặt yêu cầu/xác nhận cọc/tạo hóa đơn đồng thời, UNIQUE ở database,
rollback sự kiện, chủ sở hữu, tài khoản LOCKED, vòng đời hợp đồng,
chuỗi công tơ, người thuê kế tiếp cùng phòng và khiếu nại/thanh toán.
