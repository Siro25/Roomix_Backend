# Reset dữ liệu demo Roomix

Script này chỉ xóa dữ liệu nghiệp vụ thuộc nhóm tài khoản demo, sau đó nạp lại `Data.sql` cho Core Service và `RentalData.sql` cho Rental Service. Nó không truncate database và không xóa tài khoản thật nằm ngoài dải seed.

Các file SQL được copy trực tiếp vào container PostgreSQL trước khi chạy, nên dữ liệu tiếng Việt UTF-8 được giữ nguyên trên Windows.

## Chạy

Từ thư mục `Roomix_Backend`:

```powershell
.\scripts\reset-demo-data.ps1
```

Gõ `RESET` khi script yêu cầu xác nhận. Khi cần chạy tự động:

```powershell
.\scripts\reset-demo-data.ps1 -Force
```

## Trạng thái sau reset

- Tài khoản chính: `admin.seed`, `landlord.seed`, `tenant.seed`; mật khẩu `Roomix@123`.
- Tài khoản bổ sung: `landlord.demo.4` đến `landlord.demo.7` và `tenant.demo.8` đến `tenant.demo.19`, dùng cùng mật khẩu.
- Core có 7 nhà, 14 tầng, 42 phòng/bài đăng với đủ trạng thái `DRAFT`, `PENDING`, `APPROVED`, `REJECTED`, `HIDDEN`.
- Có sẵn hình ảnh, yêu thích, thông báo, báo cáo vi phạm và lịch sử audit để demo các màn hình Admin/Tenant.
- Rental có yêu cầu thuê, hợp đồng `PENDING`/`ACTIVE`/`ENDED`, hóa đơn `PENDING`/`CONFIRMED`/`PAID`, nhắc thanh toán, contact card và event đã phát.
- Phòng `101` vẫn là phòng `APPROVED`, chưa thuê; phù hợp để demo tìm kiếm và gửi yêu cầu mới.
