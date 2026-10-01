# Kiểm thử CRUD phòng

Từ thư mục gốc repo:

```bash
mvn -f core-service/pom.xml -Dtest=RoomApiIntegrationTest test
```

Chạy toàn bộ Core:

```bash
mvn -f core-service/pom.xml test
```

`RoomApiIntegrationTest` chạy HTTP → JWT thật → Service → Repository → database H2 riêng. Flyway chạy V1–V6, Hibernate kiểm tra schema. Không mock service/repository, không cần PostgreSQL bên ngoài. Mỗi request có transaction thật; test kiểm tra rollback và thao tác đồng thời.

| Nhóm | Tình huống |
|---|---|
| CRUD | Tạo, đọc, PATCH, xóa; HTTP status, Location, timestamps, trạng thái AVAILABLE mặc định |
| Validation | Thiếu/null, số không dương, precision/scale, chiều dài, kiểu JSON sai, trường lạ/trường server quản lý |
| PATCH | Giữ trường bỏ qua, xóa mô tả bằng null, false khác với không gửi, không đổi tầng/status |
| Phân quyền | Từng API từ chối thiếu/sai JWT và TENANT/ADMIN; ID của chủ khác trả 404 |
| Danh sách | Lọc đúng chủ, nhà/tầng/trạng thái, khoảng giá bao gồm biên, keyword literal, sort và phân trang |
| Query sai | ID/enum/kiểu số sai, khoảng giá đảo, sort không hợp lệ, offset quá lớn, nhà/tầng không khớp |
| Số phòng | Trim/chữ hoa; trùng trong tầng bị chặn; cùng số ở tầng khác được phép; sửa trùng rollback |
| Ràng buộc | Phòng thật chặn xóa tầng/nhà, UNIQUE và FK database, dữ liệu phụ thuộc chặn xóa phòng |
| Trạng thái | OCCUPIED/RESERVED chặn xóa; AVAILABLE/MAINTENANCE cho xóa nếu không có phụ thuộc Core |
| Sức chứa | Giảm khi OCCUPIED/RESERVED trả 503 do chưa có Rental xác minh số người, giữ nguyên dữ liệu |
| Đồng thời | Tạo trùng, đổi hai phòng về cùng mã, PATCH hai trường, xóa trùng, tạo phòng đồng thời xóa tầng |

Theo lựa chọn của người dùng, **DELETE hiện chỉ kiểm tra Core**. Test không khẳng định không có hợp đồng/công nợ ở Rental. Phải bổ sung kiểm tra Rental và cơ chế phối hợp trước khi tích hợp thuê phòng.

Bảng `test_room_dependents` chỉ dùng trong test, mô phỏng FK của module bài đăng/dữ liệu phụ thuộc chưa triển khai. Quan hệ nhà–tầng–phòng được kiểm tra trên các bảng và entity thật.

H2 PostgreSQL mode không thay thế kiểm thử PostgreSQL thực tế, đặc biệt về khóa/concurrency. Sau merge, AccountStatusFilter kiểm tra trạng thái tài khoản trên mỗi request đã xác thực. MergedWorkflowTest kiểm tra JWT cũ bị từ chối sau khi Admin khóa tài khoản và dùng lại được sau khi mở khóa.
