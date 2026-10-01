# Kiểm thử API tầng

Chạy từ thư mục gốc repo:

```bash
mvn -f core-service/pom.xml test
```

Chỉ chạy phần tầng:

```bash
mvn -f core-service/pom.xml -Dtest=FloorApiIntegrationTest test
```

`FloorApiIntegrationTest` kiểm tra toàn bộ chuỗi HTTP → JWT/Security → Controller → Service → JPA → database H2 riêng trong RAM. Chạy migration V1, V2, V3 và Hibernate schema validation. Không cần PostgreSQL bên ngoài, không dùng mock service/repository. Mỗi request ghi dữ liệu trong transaction thật để kiểm tra commit/rollback; dữ liệu được dọn sau từng test.

| Nhóm | Nội dung |
|---|---|
| CRUD | Tạo tầng trệt, đọc, sửa số/tên/mô tả, xóa; Location và HTTP status |
| PATCH | Giữ trường không gửi, xóa mô tả bằng null, không nhận null cho trường bắt buộc, không đổi nhà |
| Validation | Thiếu trường, null, chuỗi rỗng, sai JSON/UUID/kiểu số, số âm/quá lớn, giới hạn độ dài |
| Danh sách | Nhà rỗng, lọc đúng nhà, phân trang, sort, trang ngoài phạm vi, offset vượt giới hạn JPA và biên hợp lệ (cả nhà và tầng) |
| HTTP | Sai method trả 405 kèm Allow; sai content type trả 415; route không tồn tại trả 404 |
| Kiểu chuỗi | POST/PATCH từ chối số, boolean, object, array cho tên/mô tả tầng; dữ liệu cũ không thay đổi |
| Security | Cả 5 API: thiếu/sai JWT, TENANT/ADMIN, chủ trọ khác, tài nguyên không tồn tại |
| Trùng số tầng | Trùng khi tạo/sửa trả 409; giữ nguyên số của chính tầng được phép; khác nhà được phép |
| Database | UNIQUE nhà/số tầng, FK nhà RESTRICT, không tạo tầng mồ côi |
| Tổng số tầng | Đếm bản ghi thay vì số tầng cao nhất; tạo/xóa cập nhật cùng transaction, lỗi không đổi số đếm |
| Phụ thuộc | Nhà có tầng không được xóa; tầng có bản ghi con tham chiếu không được xóa và transaction rollback |
| Đồng thời | Tạo khác số/cùng số, đổi hai tầng về cùng số, xóa trùng, tạo tầng đồng thời xóa nhà, PATCH hai trường độc lập |

Test phụ thuộc tại đây dùng bảng **chỉ dành cho test** `test_room_refs` với FK RESTRICT để kiểm tra đường xử lý lỗi xóa tầng. Migration V4 đã bổ sung `rooms.floor_id → floors.id ON DELETE RESTRICT`; `RoomApiIntegrationTest` kiểm tra bổ sung với phòng thật, bao gồm rollback khi xóa tầng có phòng và tạo phòng đồng thời xóa tầng.

H2 ở PostgreSQL mode không thay thế kiểm thử PostgreSQL thực tế, nhất là hành vi khóa và truy cập đồng thời. Bộ test này bao phủ các nhóm trên, không khẳng định bao phủ mọi tổ hợp đầu vào có thể có.
