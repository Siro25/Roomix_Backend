# Hợp nhất property-room-management với dev_core_gateway

Database được giữ theo lịch sử của `feature/property-room-management`, theo xác nhận của người dùng.

## Migration

- Giữ nguyên V1 users, V2 houses, V3 floors, V4 rooms, bao gồm checksum.
- V5 chỉ bổ sung audit_logs và notifications; không tạo lại bảng nhà/tầng/phòng.
- V6 bổ sung posts, tham chiếu users và rooms đang có.
- Database đã chạy V4 của feature sẽ được Flyway nâng lên V6 khi Core khởi động. Database mới chạy lần lượt V1–V6.
- Database đã chạy V2/V3 cũ của dev_core_gateway có lịch sử khác: cần phương án chuyển đổi riêng trước khi dùng bản merge này. Không dùng Flyway repair để che khác biệt schema.

## Luồng hoạt động

Gateway chuyển `/api/houses/**`, `/api/floors/**`, `/api/rooms/**` về Core. Core kiểm tra JWT, trạng thái tài khoản và role; service kiểm tra quyền sở hữu tài nguyên.

Chủ trọ đăng ký ở trạng thái chờ duyệt, chưa nhận token. Admin duyệt thì chủ trọ đăng nhập và quản lý nhà/tầng/phòng được. Khóa tài khoản khiến JWT còn hạn cũng bị từ chối. AccountStatusFilter chỉ chạy trong Spring Security sau bước xác thực JWT, không đăng ký thêm như servlet filter.

Entity nhà/tầng/phòng giữ các trường và ràng buộc của feature, bổ sung quan hệ đọc cho Admin xem tài sản. Không cascade xóa nhà/tầng/phòng. Phòng được bài đăng tham chiếu không thể bị xóa. Kiểm tra hợp đồng Rental khi xóa phòng vẫn được hoãn theo phạm vi đã thống nhất.

Phân trang CRUD giữ `items`; phân trang Admin giữ `content`, `first`, `last` qua AdminPageResponse để tránh đổi hợp đồng response của hai nhánh.

## Kiểm thử

```bash
mvn -f core-service/pom.xml test
mvn -f api-gateway/pom.xml test
```

FeatureDatabaseUpgradeTest kiểm tra nâng V4 lên V6, giữ dữ liệu cũ và validate lịch sử Flyway. MergedWorkflowTest kiểm tra đăng ký, duyệt chủ trọ, CRUD tài sản, Admin đọc tài sản/duyệt bài, khóa và mở khóa tài khoản, audit và thông báo.

Test database dùng H2 PostgreSQL mode; chưa thay thế kiểm thử trên PostgreSQL thật. Gateway test kiểm tra khởi tạo context; chưa kiểm thử proxy liên service qua mạng. Không thay đổi database đang chạy trong quá trình giải quyết merge.
