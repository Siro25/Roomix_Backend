# Pipeline AI và liveness hiện tại

File chính vẫn là `src/face_auth/ai/face_service.py`, được phát triển tiếp
từ code ban đầu. Các mô hình YOLO, MTCNN và FaceNet được giữ lại.

## Một ảnh được xử lý thế nào?

1. `image_input.py` kiểm tra giới hạn file/pixel và định dạng JPEG/PNG/WebP
   tĩnh; áp dụng EXIF orientation và mã hóa lại JPEG cho thông báo nếu cần.
2. `face_service.py` chạy YOLO như pipeline cũ, đồng thời dùng MTCNN kiểm tra
   toàn ảnh có đúng một mặt. Không tự chọn một người trong ảnh nhiều người.
3. Kiểm tra confidence, kích thước, vùng mặt nằm trong ảnh, độ sáng, tương
   phản và độ mờ. Ngưỡng Laplacian hiện là 40, cần hiệu chỉnh theo camera.
4. `head_pose.py` tính hướng quay dựa trên vị trí mũi và trục hai mắt; từ
   chối landmarks không hợp lệ và đầu nghiêng quá 25 độ.
5. MTCNN cắt đúng box vừa được kiểm tra; FaceNet tạo vector 512 chiều, sau
   đó chuẩn hóa. Không detect lại rồi mặc định lấy mặt đầu tiên. Vector rỗng,
   NaN, vô cực hoặc sai kích thước bị từ chối.

Cách tách `detect()` và `extract()` theo API của
[mã nguồn facenet-pytorch](https://github.com/timesler/facenet-pytorch/blob/master/models/mtcnn.py).
Các ngưỡng chất lượng và hướng quay là lựa chọn MVP của dự án, không phải
thông số bảo đảm độ chính xác từ thư viện.

## Thử thách quay đầu

- Trình tự: chính diện, quay hai hướng theo thứ tự ngẫu nhiên, chính diện.
- Mỗi bước cần hai frame liên tiếp đạt và cách nhau ít nhất 200 ms phía server.
- Mỗi bước tối đa 12 giây từ frame đầu hoặc khi bước trước hoàn tất.
- Frame sai hướng xóa số lần đạt liên tiếp của bước hiện tại.
- Vector hồ sơ chỉ lấy khi cần nhìn chính diện và frame thực sự chính diện.
- Reference danh tính được chốt từ frame chính diện, không từ mặt quay đầu
  gửi trước khi bắt đầu đúng thử thách. Giữ kiểm tra cùng người xuyên suốt.
- Quá thời gian bước trả `LIVENESS_TIMEOUT`; LOGIN thất bại sẽ gửi ảnh đúng
  lượt thử sau quyết định Core. ENROLL thất bại không gửi ảnh Telegram.

Trái/phải là hướng trên **ảnh gốc gửi lên**. Nếu frontend lật phần xem trước
bằng CSS, cần hiển thị hướng dẫn tương ứng, không tự lật canvas gửi server.

## Đã kiểm tra và chưa kiểm tra

`tests/test_face_service.py` chạy giải mã/kiểm tra ảnh và tính chất lượng bằng
Pillow, NumPy, OpenCV thật; detector/encoder được thay bằng fake. Test xác
nhận đúng box, số mặt, điều kiện từ chối và shape/chuẩn hóa embedding.

`tests/test_liveness.py` kiểm tra góc quay, đầu nghiêng, thứ tự thử thách,
số frame liên tiếp và timeout. `tests/test_workflow.py` kiểm tra kết quả
liveness được đưa vào luồng và sự kiện Telegram đúng cách.

Đã chạy kiểm tra khởi tạo và inference thật trên CPU bằng tensor và ảnh
trống (xem `RUNTIME.md`). Chưa thử webcam hoặc ảnh người thật.
Không dùng test fake để kết luận độ chính xác nhận dạng. Trước demo cần thử
người đã đăng ký, người chưa đăng ký, nhiều người, ánh sáng và webcam thực tế.

Đây là liveness chuyển động mức MVP. Chưa có mô hình chống giả mạo chuyên
dụng, không bảo đảm chống ảnh/video phát lại, deepfake hoặc camera ảo.
