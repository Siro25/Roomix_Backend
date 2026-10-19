# Market data

Đặt CSV do crawler tạo tại `phongtro_hanoi.csv`, hoặc cấu hình `MARKET_DATA_CSV` trong `.env` để trỏ tới một CSV khác.

Service đọc các cột `gia_thue`, `quan_huyen`, `tien_nghi`, `tieu_de` và `dien_tich`. CSV không được chép từ máy nguồn trong lần sắp xếp này; pricing API sẽ bỏ qua phần đánh giá theo dataset cho tới khi có file CSV hợp lệ.
