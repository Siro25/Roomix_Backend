## 1.Cấu trúc thư mục của rental-service
```
rental-service/
└── src/main/java/com/team4/rental/
    │
    ├── controllers/        # Tầng giao tiếp (REST API)
    │   ├── RentalRequestController.java
    │   ├── TenancyController.java
    │   ├── InvoiceController.java
    │   └── ChatController.java
    │
    ├── services/           # Tầng nghiệp vụ xử lý logic giao dịch
    │   ├── RentalRequestService.java # Kiểm tra phòng trống từ Core và xử lý Accept/Reject
    │   ├── TenancyService.java       # Quản lý vòng đời thuê, kiểm tra ACTIVE_TENANCY chống double-booking
    │   ├── InvoiceService.java       # Tính tiền điện/nước/phòng nối tiếp theo kỳ của từng tenancy
    │   └── ChatService.java          # Xử lý tin nhắn thời gian thực[cite: 1]
    │
    ├── repositories/       # Tầng giao tiếp trực tiếp với rental_db[cite: 1]
    │   ├── RentalRequestRepository.java
    │   ├── TenancyRepository.java
    │   ├── ActiveTenancyRepository.java # Nơi chốt chặn UNIQUE roomId để ngăn thuê trùng[cite: 1]
    │   ├── InvoiceRepository.java
    │   ├── InvoiceItemRepository.java
    │   ├── ConversationRepository.java
    │   └── MessageRepository.java
    │
    ├── entities/           # Ánh xạ các bảng trong rental_db[cite: 1]
    │   ├── RentalRequest.java
    │   ├── Tenancy.java
    │   ├── ActiveTenancy.java
    │   ├── Invoice.java
    │   ├── InvoiceItem.java
    │   ├── Conversation.java
    │   └── Message.java
    │
    ├── dtos/               # Đối tượng truyền tải dữ liệu
    │   ├── request/        # Ví dụ: SendRentalRequest, CreateInvoiceRequest
    │   └── response/       # Ví dụ: InvoiceDetailResponse, ChatMessageResponse
    │
    ├── enums/              # Các trạng thái máy (State Machine) của Rental[cite: 1]
    │   ├── RequestStatus.java # PENDING, ACCEPTED, REJECTED[cite: 1]
    │   ├── TenancyStatus.java # PENDING, CANCELLED, ACTIVE, ENDED[cite: 1]
    │   └── InvoiceStatus.java # PENDING, CONFIRMED, PAID (Kèm cờ isDisputed)[cite: 1]
    │
    ├── config/             # Cấu hình Bean
    │   ├── RabbitMQConfig.java # Cấu hình phát sự kiện (Producer)[cite: 1]
    │   ├── WebSocketConfig.java# Cấu hình Chat WebSocket (/topic/chat/{conversationId})[cite: 1]
    │   └── SecurityConfig.java # Xác thực JWT do Core truyền sang
    │
    ├── events/             # Xử lý thông điệp gửi đi
    │   └── publishers/     # Gắn logic bắn sự kiện TENANCY_ACTIVATED, TENANCY_ENDED, INVOICE_CREATED sang Core[cite: 1]
    │
    ├── external/           # (Hoặc 'clients') Gọi API nội bộ sang Core Service
    │   └── CoreServiceClient.java # Gọi GET /internal/rooms/{roomId} để kiểm tra phòng hợp lệ trước khi cho thuê[cite: 1]
    │
    └── exceptions/         # Bắt lỗi toàn cục
        ├── GlobalExceptionHandler.java
        ├── DoubleBookingException.java # Lỗi khi phòng đã có ACTIVE_TENANCY[cite: 1]
        └── InvalidMeterReadingException.java # Lỗi khi chỉ số điện/nước mới nhỏ hơn chỉ số cũ[cite: 1, 3]
```
