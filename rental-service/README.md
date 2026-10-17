# Rental Service

Nghiệp vụ yêu cầu thuê, hợp đồng và hóa đơn. Package gốc: `com.team4.rental`.
Code sử dụng Lombok và builder, xử lý trực tiếp trong từng phương thức service,
không thêm lớp helper/utility hoặc hàm mapper dùng chung.

## Phạm vi hiện tại

- Yêu cầu thuê: tạo, xem chi tiết/danh sách có bộ lọc; khách hủy khi PENDING;
  chủ trọ chấp nhận hoặc từ chối.
- Chấp nhận tạo hợp đồng PENDING và từ chối mọi yêu cầu PENDING khác của cùng phòng trong một transaction.
- Hợp đồng: PENDING → ACTIVE khi chủ trọ xác nhận cọc; PENDING → CANCELLED;
  ACTIVE → ENDED khi chủ trọ xác nhận trả phòng. Không tự kết thúc chỉ vì ngày trên lịch đã qua:
  cần hoàn tất quyết toán và xác nhận bàn giao thực tế.
- Hóa đơn: PENDING → CONFIRMED bởi khách thuê → PAID bởi chủ trọ.
- Khiếu nại: khách thuê đánh dấu disputed và ghi lý do; chủ trọ giải quyết kèm nội dung,
  sau đó khách phải xác nhận lại.
- Đã bỏ AMQP/RabbitMQ dependency khỏi Core và Rental.
- Có scheduler giải phóng giữ chỗ quá hạn và lưu nhắc thanh toán quá hạn.
- Có publisher Kafka đọc transactional outbox; Core nhận sự kiện để đồng bộ trạng thái phòng.
- Quyết toán: đối soát công nợ, chốt chỉ số cuối, bù trừ cọc, ghi nhận thu đủ/hoàn đủ rồi mới ENDED.
  API end cũ bị chặn nếu chưa có phiếu quyết toán.
- Điều chỉnh hóa đơn: chỉ kỳ mới nhất chưa thu tiền của hợp đồng ACTIVE, bắt buộc lý do,
  lưu bản trước/sau và yêu cầu khách xác nhận lại.

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
| Sự kiện thuê | tenancy_events lưu cùng transaction; publisher gửi Kafka sau commit |
| Lịch sử điều chỉnh | invoice_corrections lưu invoice, meterReading, items trước/sau, revision, người sửa và lý do |
| Quyết toán | tenancy_settlements lưu chỉ số cuối, công nợ, phí bổ sung, cọc, tiền thu/hoàn; UNIQUE contract_id |

UUID user/room/post là tham chiếu sang Core, không tạo foreign key xuyên hai database.
Foreign key giữa các bảng trong rental_db vẫn được tạo đầy đủ.
Lịch sử hợp đồng và hóa đơn được giữ lại; kết thúc thuê chỉ xóa bản ghi active_tenancies.

## Chạy local

Tạo database rỗng `rental_db` trong PostgreSQL. Flyway áp dụng migration theo thứ tự,
Hibernate chỉ validate schema. Database đã có V1 sẽ chỉ chạy các migration V2–V5 còn thiếu:

- V1: các bảng nghiệp vụ gốc, giữ nguyên.
- V2: thêm deposit_deadline; hợp đồng PENDING cũ được thêm 24 giờ kể từ lúc migrate.
- V3: bảng invoice_reminders và các index phục vụ truy vấn quá hạn.
- V4: invoices.revision, invoice_corrections, tenancy_settlements và ràng buộc số tiền quyết toán.
- V5: cột retry/backoff và index giữ thứ tự gửi sự kiện theo roomId.

Không sửa migration sau khi đã áp dụng; thay đổi tiếp phải thêm phiên bản mới.
Core có migration V6 tạo bảng `processed_rental_events`. Hai migration mới sẽ được Flyway
áp dụng khi khởi động service tương ứng; không xóa hoặc chỉnh các migration cũ.

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
| GET | /api/rentals/{requestId} | Chi tiết, chỉ người gửi hoặc chủ trọ nhận yêu cầu |
| POST | /api/rentals/{requestId}/cancel | TENANT sở hữu yêu cầu; chỉ PENDING; gọi lặp CANCELLED trả dữ liệu hiện có |
| POST | /api/rentals/{requestId}/accept | LANDLORD sở hữu phòng; trả hợp đồng vừa tạo |
| POST | /api/rentals/{requestId}/reject | LANDLORD sở hữu phòng; reason bắt buộc |
| GET | /api/tenancies | Hợp đồng thuộc người gọi |
| GET | /api/tenancies/{tenancyId} | Chỉ hai bên hợp đồng |
| POST | /api/tenancies/{tenancyId}/confirm-deposit | LANDLORD xác nhận đã nhận đủ cọc theo hợp đồng |
| POST | /api/tenancies/{tenancyId}/cancel | Hai bên hợp đồng; chỉ PENDING, reason bắt buộc |
| POST | /api/tenancies/{tenancyId}/end | LANDLORD; chặn 409 nếu chưa quyết toán; không dùng để bỏ qua quyết toán |
| POST | /api/tenancies/{tenancyId}/settlement/preview | LANDLORD; tính thử, chưa thu tiền/chưa lưu phiếu |
| POST | /api/tenancies/{tenancyId}/settlement | LANDLORD; thu đủ/hoàn đủ, lưu phiếu và chuyển ENDED cùng transaction |
| GET | /api/tenancies/{tenancyId}/settlement | Hai bên hợp đồng xem phiếu đã hoàn tất |
| POST | /api/invoices | LANDLORD; hợp đồng ACTIVE, trả 201 |
| GET | /api/invoices | Hóa đơn thuộc người gọi |
| GET | /api/invoices/{invoiceId} | Chi tiết hóa đơn, chỉ số và ba dòng phí |
| GET | /api/invoices/reminders | Nhắc quá hạn mới nhất của từng hóa đơn thuộc người gọi, có phân trang |
| POST | /api/invoices/{invoiceId}/remind | LANDLORD sở hữu hóa đơn; nhắc quá hạn tối đa một lần mỗi ngày |
| POST | /api/invoices/{invoiceId}/confirm | TENANT của hợp đồng |
| POST | /api/invoices/{invoiceId}/pay | LANDLORD; chỉ CONFIRMED và không khiếu nại |
| POST | /api/invoices/{invoiceId}/dispute | TENANT; chưa PAID, reason bắt buộc |
| POST | /api/invoices/{invoiceId}/resolve-dispute | LANDLORD; reason là nội dung giải quyết |
| POST | /api/invoices/{invoiceId}/corrections | LANDLORD; sửa kỳ mới nhất chưa thu tiền, có expectedRevision |
| GET | /api/invoices/{invoiceId}/corrections | Hai bên hợp đồng xem lịch sử, phân trang page/size |

Không thuộc quyền sở hữu trả 404. Sai vai trò trả 403. Sai trạng thái hoặc đặt trùng trả 409.
Xác nhận cọc, kết thúc, hủy và thu tiền lặp lại trả trạng thái hiện có, không tạo thêm sự kiện/thu tiền.

### Bộ lọc danh sách

Mọi bộ lọc đều kết hợp điều kiện người gọi là một bên của yêu cầu/hợp đồng.
Không truyền bộ lọc vẫn chỉ thấy dữ liệu của mình.

| Endpoint | Bộ lọc tùy chọn |
| --- | --- |
| GET /api/rentals | status (PENDING/ACCEPTED/REJECTED/CANCELLED), roomId |
| GET /api/tenancies | status (PENDING/ACTIVE/ENDED/CANCELLED), roomId |
| GET /api/invoices | status (PENDING/CONFIRMED/PAID), roomId, tenancyId, billingMonth (YYYY-MM), overdue (true/false) |

Ví dụ: `GET /api/invoices?billingMonth=2026-10&overdue=true&page=0&size=20`.
Sai enum hoặc định dạng tháng trả 400.
Danh sách hóa đơn trả InvoiceSummaryResponse trong Page, gồm id, invoiceCode, tenancyId,
billingMonth, dueDate, status, totalAmount, paidAmount, outstandingAmount, overdue, disputed.
Frontend cần cập nhật nếu trước đây đang đọc các trường khác trực tiếp từ entity Invoice.
Chi tiết hóa đơn giữ invoice/meterReading/items và bổ sung overdue, outstandingAmount.

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

### Hạn xác nhận cọc

- Không gửi depositDeadline (hoặc gửi null): mặc định 24 giờ tính từ thời điểm accept.
- Chủ trọ có thể thêm `"depositDeadline":"2026-10-15T18:00:00+07:00"` vào payload accept.
  Thay ngày mẫu bằng thời điểm tương lai phù hợp; ngày của hạn không được sau endDate.
- Hợp đồng trả về depositDeadline dạng Instant; FE đổi sang múi giờ hiển thị.
- Tại đúng hạn hoặc sau hạn, confirm-deposit trả 409 DEPOSIT_EXPIRED nếu hợp đồng còn PENDING.
  Nếu scheduler đã chuyển CANCELLED thì trả lỗi trạng thái 409.
- Scheduler chuyển PENDING hết hạn sang CANCELLED, ghi lý do, giải phóng giữ chỗ.
  ACTIVE không bị tác động. Gửi/accept yêu cầu mới cũng dọn giữ chỗ hết hạn của cùng phòng
  trong transaction thành công, không cần đợi scheduler.
- Yêu cầu cũ giữ trạng thái ACCEPTED để bảo toàn lịch sử; hợp đồng tương ứng là CANCELLED.
  Không tự mở lại các yêu cầu đã bị từ chối; khách cần gửi yêu cầu mới.

### Quá hạn và nhắc thanh toán

- Quá hạn khi dueDate < ngày hiện tại và paidAmount < totalAmount;
  ngay trong ngày đến hạn chưa coi là quá hạn. Không thêm trạng thái OVERDUE vào enum.
- outstandingAmount = totalAmount - paidAmount. Lịch nghiệp vụ dùng Asia/Ho_Chi_Minh.
- Scheduler lưu nhắc vào invoice_reminders, tối đa một bản ghi mỗi hóa đơn/ngày.
  Chủ trọ gọi API remind nhiều lần cùng ngày cũng nhận lại bản ghi đó.
- GET reminders chỉ hiển thị bản nhắc gần nhất mỗi hóa đơn còn quá hạn/chưa thu đủ,
  luôn giới hạn theo tenantId/landlordId của người gọi. Thanh toán xong không còn hiển thị,
  nhưng lịch sử vẫn nằm trong database.
- Đây là dữ liệu nhắc để FE lấy qua REST, **chưa gửi email, WebSocket hoặc Notification tại Core**.
  Không phát Kafka. outstandingAmount trong bản nhắc là số dư tại lúc tạo nhắc.
- Hai scheduler mặc định chạy lại sau mỗi 60 giây, xử lý tối đa 100 mục/lượt.
  Khóa database và UNIQUE chống tạo nhắc trùng khi chạy nhiều instance.
  Cấu hình qua `rental.jobs.enabled`, `rental.jobs.expiry-delay-ms`,
  `rental.jobs.reminder-delay-ms`; đặt enabled=false để tắt cả hai.

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
  Sửa số tiền/chỉ số phải qua API corrections có lưu lịch sử.
  API resolve-dispute chỉ ghi nội dung đối soát, không tự thay tiền hoặc tự đánh dấu PAID.

## 1.4. Quyết toán trả phòng

### 1.4.1. Tính thử trước khi thu/hoàn tiền

Gửi `POST /api/tenancies/{tenancyId}/settlement/preview`:

```json
{
  "actualReturnDate": "2026-10-14",
  "finalElectricity": 112,
  "finalWater": 23,
  "damageCharge": 100000,
  "damageReason": "Thay khóa bị hỏng theo biên bản bàn giao"
}
```

- Chỉ chủ trọ của hợp đồng ACTIVE được thực hiện. Ngày trả không trước startDate hoặc sau hôm nay.
- Chỉ số cuối không thấp hơn chỉ số mới của kỳ gần nhất; nếu chưa có hóa đơn thì so với chỉ số đầu hợp đồng.
- Không được trả phòng trước readingDate của hóa đơn gần nhất.
- damageCharge > 0 bắt buộc có damageReason. Không tự trừ cọc với lý do mặc định.
- Preview chưa lưu phiếu, chưa đổi trạng thái và chưa ghi nhận thu/hoàn tiền.

### 1.4.2. Các khoản tính toán

| Trường | Cách tính |
| --- | --- |
| invoiceDebt | Tổng totalAmount - paidAmount của các hóa đơn thuộc hợp đồng |
| additionalRent | Số tháng chưa lập hóa đơn đến tháng trả thực tế × monthlyRent |
| additionalElectricity / additionalWater | Phần chỉ số cuối vượt kỳ đã lập hóa đơn × đơn giá trong hợp đồng |
| totalDue | invoiceDebt + additionalRent + additionalElectricity + additionalWater + damageCharge |
| depositApplied | min(depositAmount, totalDue) |
| amountToCollect | totalDue - depositApplied |
| amountToRefund | depositAmount - depositApplied |

Giữ chính sách tiền phòng **nguyên tháng**, chưa chia theo ngày. Tháng đã có hóa đơn không bị tính lại
trong additionalRent, kể cả khi hóa đơn chưa thanh toán. Nếu thực tế trả sau endDate nhưng hợp đồng vẫn ACTIVE,
quyết toán tính tiếp các tháng chưa lập hóa đơn đến ngày trả theo monthlyRent hiện tại.
Các phí cuối và chỉ số cuối nằm trong phiếu quyết toán riêng, không sửa hóa đơn đã PAID,
không chèn thêm kỳ trùng vào meter_readings.

Ví dụ: hóa đơn còn nợ 3.065.000, điện bổ sung 7.000, nước 15.000, thiệt hại 100.000:
totalDue = 3.187.000; cọc 3.000.000; cần thu thêm 187.000, không hoàn cọc.
Nếu mọi hóa đơn đã trả và không phát sinh thêm thì hoàn lại toàn bộ cọc.

### 1.4.3. Hoàn tất quyết toán

Sau khi chủ trọ thực sự nhận đủ tiền và hoàn cọc thừa, gửi
`POST /api/tenancies/{tenancyId}/settlement`:

```json
{
  "details": {
    "actualReturnDate": "2026-10-14",
    "finalElectricity": 112,
    "finalWater": 23,
    "damageCharge": 100000,
    "damageReason": "Thay khóa bị hỏng theo biên bản bàn giao"
  },
  "expectedTotalDue": 3187000,
  "collectedAmount": 187000,
  "refundedAmount": 0,
  "paymentMethod": "BANK_TRANSFER"
}
```

- Các con số mẫu chỉ đúng với ví dụ trên; FE phải lấy số thực tế từ preview.
- Server khóa phòng → hợp đồng → hóa đơn, tính lại tiền; không tin tổng tiền do FE gửi.
- collectedAmount phải bằng amountToCollect, refundedAmount phải bằng amountToRefund,
  expectedTotalDue phải khớp kết quả tính lại. Thiếu, thừa hoặc preview cũ trả 409 PAYMENT_MISMATCH.
- Hóa đơn chưa trả phải CONFIRMED, không còn khiếu nại; nếu vừa sửa phải được khách xác nhận lại.
- Cùng transaction: lưu phiếu → cập nhật hóa đơn còn nợ sang PAID → ENDED →
  xóa active_tenancies → lưu TENANCY_ENDED vào outbox. Lỗi ở bất kỳ bước nào đều rollback.
- Hóa đơn trả qua quyết toán có paymentMethod = SETTLEMENT; phương thức nhận/hoàn thực tế
  (CASH hoặc BANK_TRANSFER), số tiền cọc và công nợ trước khi thu được lưu trên phiếu.
  Hóa đơn đã trả trước đó giữ nguyên phương thức/ngày thanh toán.
- Gọi lại hoặc gọi đồng thời cùng hợp đồng trả phiếu đã lưu; không thu/hoàn hoặc phát sự kiện lần nữa.
  Phiếu đã hoàn tất không có API sửa. Muốn xem dùng GET settlement, có kiểm tra chủ sở hữu.
- Đây là chủ trọ **ghi nhận** tiền đã nhận/hoàn, không phải tích hợp cổng thanh toán hay tự kiểm tra ngân hàng.
  Hợp đồng ENDED từ trước migration không bị tạo phiếu quyết toán giả.

## 1.5. Điều chỉnh hóa đơn

### 1.5.1. Sửa kỳ mới nhất chưa thanh toán

Gửi `POST /api/invoices/{invoiceId}/corrections`:

```json
{
  "expectedRevision": 0,
  "reason": "Sửa chỉ số theo ảnh công tơ và biên bản đối soát",
  "readingDate": "2026-10-14",
  "dueDate": "2026-10-21",
  "roomRent": 3000000,
  "electricityUnitPrice": 3500,
  "waterUnitPrice": 15000,
  "newElectricityVal": 120,
  "newWaterVal": 23
}
```

- expectedRevision lấy từ invoice.revision trong GET chi tiết hóa đơn, ban đầu bằng 0.
  Mỗi lần sửa tăng 1; revision cũ trả 409 để tránh ghi đè thay đổi của lần sửa khác.
- Chỉ chủ trọ sở hữu, hợp đồng ACTIVE, hóa đơn kỳ mới nhất, status khác PAID và paidAmount = 0.
- Không cho đổi hợp đồng, kỳ hóa đơn, chỉ số cũ hoặc ID công tơ; số mới phải >= số cũ.
  Ngày ghi số nằm trong kỳ và thời gian hợp đồng, không sau hôm nay; dueDate >= readingDate.
- Giá/tiền phòng được sửa cho **hóa đơn này**, không sửa đơn giá hợp đồng hay hóa đơn kỳ khác.
  Phần tiêu thụ phát sinh ở quyết toán vẫn dùng đơn giá của hợp đồng.
- Cập nhật đồng thời chỉ số, ba dòng phí và tổng tiền; dùng BigDecimal, làm tròn HALF_UP.
- Chuyển về PENDING, xóa confirmedAt; khách phải xác nhận lại trước khi thu tiền/quyết toán.
  Nếu đang khiếu nại thì giữ cờ disputed; chủ trọ vẫn phải resolve-dispute có giải thích.

### 1.5.2. Lịch sử và tính toàn vẹn

- `GET /api/invoices/{invoiceId}/corrections?page=0&size=20` trả lịch sử mới nhất trước,
  chỉ hai bên hợp đồng được xem.
- Mỗi bản ghi lưu revision, correctedBy từ JWT, reason, createdAt, oldValues/newValues
  gồm invoice + meterReading + items. Snapshot dùng kiểu BigDecimal để giữ chính xác số tiền.
- Không có API sửa/xóa lịch sử. Snapshot và dữ liệu hóa đơn được commit/rollback cùng nhau.
- Sửa hóa đơn và tạo kỳ tiếp theo cùng khóa hợp đồng: không tạo chuỗi công tơ sai khi thao tác đồng thời.
- Không sửa kỳ cũ hoặc hóa đơn PAID; chưa có chứng từ bù trừ cho các trường hợp đó, đúng phạm vi đã chốt.

## Chống tranh chấp đồng thời

1. Khóa hàng room_booking_locks theo roomId trước khi tạo/duyệt/từ chối yêu cầu hoặc chuyển trạng thái thuê.
2. PostgreSQL partial UNIQUE trên contracts(room_id) với status IN ('PENDING','ACTIVE')
   chặn hai hợp đồng đang giữ cùng phòng, kể cả lúc chưa nhận cọc.
3. active_tenancies.room_id UNIQUE là chốt cuối khi kích hoạt thuê.
4. Hóa đơn khóa hàng hợp đồng; kết thúc hợp đồng cũng khóa cùng hàng.
   Hai lần tạo cùng kỳ không thể tạo hai bản ghi hoặc ghi lệch chuỗi công tơ.
5. Hóa đơn có UNIQUE(contract_id, billing_month); cập nhật xác nhận, thu tiền và khiếu nại khóa hàng invoice.

## Kafka đồng bộ với Core

`TENANCY_ACTIVATED`/`TENANCY_ENDED` được ghi vào `tenancy_events` cùng transaction hợp đồng.
Scheduler gửi các hàng đã commit vào `rental-events-topic` (3 partitions), dùng `roomId`
làm Kafka key. `SELECT ... FOR UPDATE SKIP LOCKED` cho phép nhiều instance gửi song song,
nhưng không chọn sự kiện sau nếu cùng phòng còn sự kiện trước chưa được gửi.
Chỉ sau broker ACK mới điền `published_at`. Lỗi gửi ghi `attempt_count`,
`next_attempt_at`, `last_error` và tự retry; không xóa outbox.

Core đọc theo consumer group `core-room-projection`, khóa hàng ROOM, ghi
`processed_rental_events` cùng transaction với cập nhật phòng rồi mới ACK offset.
`eventId` chống xử lý trùng; `sequence` chặn sự kiện cũ ghi đè trạng thái mới.
Khi ACTIVE: `is_rented=true`, `ROOM.status=OCCUPIED`; khi ENDED:
`is_rented=false`, chỉ đổi `OCCUPIED → AVAILABLE`, không đè `MAINTENANCE/RESERVED`.
Trạng thái duyệt của `POST` không bị Kafka thay đổi. Core retry bản ghi lỗi liên tục
và giữ partition lại, nên cần xử lý dữ liệu sai thay vì bỏ qua. Đây là đồng bộ eventual consistency;
khóa và UNIQUE ở Rental vẫn là nguồn bảo vệ chống thuê trùng.

Chạy Kafka local trước hai service (cần Docker Desktop đang hoạt động):

```powershell
docker compose -f compose.kafka.yml up -d
```

Mặc định hai service kết nối `localhost:9092`; nếu broker ở máy khác, đặt
`KAFKA_BOOTSTRAP_SERVERS` giống nhau cho cả hai. Nếu Core/Rental chạy trong cùng
Docker network với broker, dùng `kafka:19092` thay cho `localhost:9092`.
Compose lưu log Kafka trong named volume,
không chạy `docker compose down -v` nếu cần giữ sự kiện. Kiểm tra outbox lỗi:

```sql
SELECT sequence, event_id, room_id, event_type, attempt_count, next_attempt_at, last_error
FROM tenancy_events WHERE published_at IS NULL ORDER BY sequence;
```

Broker single-node PLAINTEXT trong compose chỉ dành cho máy phát triển; triển khai thật
cần cluster bền vững, replication và xác thực/SSL.

## Kiểm thử

Chạy `mvnw.cmd clean test` ở cả `rental-service` và `core-service`. Test của Rental
kiểm tra publisher, retry và gửi/đọc sự kiện qua Kafka nhúng (không cần Docker).
Test của Core kiểm tra chuyển trạng thái, chống trùng/sự kiện cũ, ACK sau khi xử lý
và listener nhận JSON qua Kafka nhúng. Nếu Kiro đang tự biên dịch vào `target`, chạy Maven
với `"-Droomix.build.directory=target-maven-test"` để tách thư mục build.

Để kiểm tra cả hai service với database thật: bật broker ở trên, khởi động Core và Rental,
tạo một hợp đồng PENDING qua API rồi gọi `POST /api/tenancies/{tenancyId}/confirm-deposit`.
Đợi outbox được gửi, kiểm tra:

```sql
-- rental_db
SELECT sequence, event_type, room_id, published_at, last_error
FROM tenancy_events WHERE tenancy_id = '<tenancyId>' ORDER BY sequence;

-- core_db
SELECT status, is_rented FROM rooms WHERE id = '<roomId>';
SELECT event_id, event_sequence, event_type FROM processed_rental_events
WHERE room_id = '<roomId>' ORDER BY event_sequence;
```

Sau khi hoàn tất quyết toán và kết thúc hợp đồng, Core phải có `is_rented=false`
và ROOM trở về `AVAILABLE` nếu trước đó là `OCCUPIED`. Bài đăng `POST.status` giữ nguyên.
Đây là bước kiểm tra liên service thủ công; test tự động hiện chưa khởi chạy hai database
PostgreSQL và cả hai ứng dụng cùng lúc.
RentalExtensionsIntegrationTest có 28 ca: hủy/xem chi tiết đúng quyền, accept và cancel đồng thời,
lọc dữ liệu theo người dùng, hạn mặc định và tùy chọn, từ chối hạn sai, hết hạn tại ranh giới,
gửi yêu cầu mới giải phóng giữ chỗ, bảo vệ ACTIVE, lọc hóa đơn/quá hạn/số dư,
nhắc mỗi ngày và quyền sở hữu, nhắc đồng thời, HTTP sai vai trò/ID/bộ lọc;
thêm lịch sử trước/sau và số tiền lớn chính xác, revision chống ghi đè, chặn kỳ cũ/PAID,
giữ khiếu nại, sửa/thu tiền và sửa/tạo kỳ mới đồng thời, thiếu tiền/cọc bằng 0/hoàn cọc,
công nợ/chỉ số cuối/tháng chưa lập hóa đơn, chặn end bỏ qua quyết toán,
preview cũ, hai lần quyết toán đồng thời, phân quyền và validation các API mới.
CoreServiceClient và Clock được mock; dữ liệu, migration và khóa đồng thời chạy trên PostgreSQL thật.
Các ca này không thay thế kiểm thử end-to-end qua Gateway/Core hoặc giao dịch ngân hàng thực.
