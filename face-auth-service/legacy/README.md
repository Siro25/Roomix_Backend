# Mã nguồn tham khảo

Thư mục này giữ các thành phần ban đầu chưa dùng trong luồng Admin Face Auth:

- `cv/`: công cụ webcam độc lập ban đầu.
- `dashboard/`: giao diện điểm danh cũ, chưa tích hợp API Admin mới.
- `backend/`: database điểm danh, camera server, cảnh báo cũ và script chuyển pickle.
- `maven/`: cấu hình Java dự phòng ban đầu.
- `README.original.md`: tài liệu cũ; đường dẫn/lệnh trong đó mang tính lịch sử.

Ứng dụng chính chỉ chạy bằng `src.face_auth.main:app`. Không chạy các script
legacy để phục vụ đăng nhập Admin. Dữ liệu/trọng số trong thư mục này không
được tự chuyển thành hồ sơ Admin.
