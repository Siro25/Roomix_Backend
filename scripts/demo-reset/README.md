# Reset dữ liệu demo Roomix

Script này chỉ xóa dữ liệu nghiệp vụ của ba tài khoản seed và hai phòng mẫu, sau đó nạp lại `Data.sql` cho Core Service. Nó không truncate database và không xóa các tài khoản khác.

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

- `admin.seed`, `landlord.seed`, `tenant.seed` đều ở trạng thái `ACTIVE`.
- Mật khẩu của ba tài khoản là `Roomix@123`.
- Phòng `101` có bài đăng `APPROVED`, chưa có người thuê, phù hợp để demo tìm kiếm, yêu thích và gửi yêu cầu thuê.
- Phòng `201` có bài đăng `PENDING`, phù hợp để demo Admin duyệt/từ chối bài đăng.
- Rental Service không còn rental request, hợp đồng, hóa đơn, giao dịch VNPAY, contact card, meter reading hoặc event của dữ liệu demo.
