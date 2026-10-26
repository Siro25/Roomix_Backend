# Bàn giao phần Face Auth Python

## Phạm vi đã triển khai

- Pipeline YOLO, MTCNN, FaceNet từ code ban đầu, bổ sung kiểm tra ảnh và
  liveness chuyển động mức MVP.
- Đăng ký hồ sơ theo `adminId` do Core xác thực, nhận dạng Admin 1:N.
- PostgreSQL riêng cho hồ sơ, thử thách và hàng đợi Telegram.
- Chỉ tạo sự kiện ảnh khi LOGIN bằng mặt thất bại hoặc ENROLL thành công.
- Client camera và trang thử tích hợp, mặc định tắt.
- Không kiểm tra mật khẩu và không cấp JWT.

Frontend `Roomix` hiện có đăng nhập Admin qua HTTP Core, lựa chọn đăng nhập
khuôn mặt và trang đăng ký mặt trong quản trị. Hợp đồng public Core đã có
ở `Roomix/docs/ADMIN_AUTH_INTEGRATION.md`. Phiên FACE không được dùng để đăng
ký lại mặt; cần đăng nhập mật khẩu. Chưa thử webcam người thật.

## Điểm cần phối hợp với Core

Core bên cạnh đã bổ sung module `auth`: đăng nhập mật khẩu và GET /api/users/me,
JWT HS256, kiểm tra ADMIN/ACTIVE, UUID và bootstrap Admin có bật/tắt rõ ràng.
Đã kiểm thử với PostgreSQL riêng; chưa chạy database của người dùng hoặc
qua frontend/Gateway thật. Xem core-service/docs/ADMIN_AUTH.md.

Core đã có các API tạo phiên, validate, callback và complete nhận JWT theo
CORE_INTEGRATION.md. Core lưu hash completionToken, khóa hàng phiên để chống
ghi đè và xử lý idempotent; JWT FACE không dùng để đăng ký lại mặt.
Đã qua 63 kiểm thử Java (gồm thông báo chữ và PostgreSQL), cùng kiểm tra HTTP thật Python–Java
bằng analyzer giả lập cho ENROLL/LOGIN/failure. Còn cần cấu hình database/token
của nhóm, Gateway/Docker, bot Telegram thật và thử trình duyệt/webcam thật.
Core đã có hàng đợi thông báo chữ cho password đúng/sai và LOGIN mặt thành
công; ảnh LOGIN mặt thất bại/ENROLL thành công vẫn do Face gửi. Xem
core-service/docs/TELEGRAM.md. HTTP Telegram trong kiểm thử được giả lập.

`expiresAt` là Unix seconds hữu hạn; `adminId` nhận UUID chuẩn chữ thường
có dấu gạch nối. ID số nguyên dương vừa Java Long của dữ liệu cũ vẫn được giữ.
Face không biến ID số cũ thành UUID mới. Không gửi ID từ form đăng ký của trình duyệt.
Gateway định tuyến `/api/face/**` đến Face, không đưa API nội bộ Core ra ngoài.

## Quy ước lỗi của Face

| HTTP / mã | Ý nghĩa | Cách xử lý |
|---|---|---|
| 401 `ADMIN_LOGIN_REQUIRED` | Thiếu đăng nhập khi ENROLL | Đăng nhập mật khẩu trước |
| 403 `CORE_SESSION_REJECTED` | Core từ chối phiên | Tạo phiên hợp lệ mới |
| 409 `SESSION_ALREADY_STARTED` | Đã tạo thử thách cho phiên | Không dùng lại để bắt đầu |
| 409 `CHALLENGE_ALREADY_USED` | Face đã có kết quả cuối | GET trạng thái, không xác thực lại |
| 410 `CHALLENGE_EXPIRED` | Phiên hết hạn | Xin phiên mới |
| 429 `FRAME_TOO_FAST` | Gửi ảnh quá nhanh | Chờ rồi gửi lại |
| 503 `AI_UNAVAILABLE` | Lỗi nội bộ mô hình | Thử lại có giới hạn; không coi là nhận dạng sai |
| 503 `CORE_UNAVAILABLE` | Core lỗi/kết nối/phản hồi không hợp lệ | Thử lại cùng thử thách |
| 503 `DATABASE_UNAVAILABLE` | PostgreSQL lỗi hoặc bận | Thử lại có giới hạn |

`FAILED` trong response thành công HTTP là kết quả xác thực không đạt,
khác với lỗi hạ tầng 503. Lỗi AI không làm mất bước đang xử lý và không tạo
sự kiện Telegram nhận dạng sai. Mỗi request trình duyệt chờ tối đa 20 giây;
nếu mất response cuối, client đọc trạng thái để đối chiếu kết quả đã lưu.

Face kiểm tra lại thời hạn sau inference và trước callback Core. Các response
API Face, gồm response lỗi, được đặt `Cache-Control: no-store`.

## Chạy thử với tài khoản thật

Sau khi Core có API và cấu hình đúng PostgreSQL/token nội bộ:

1. Admin đăng nhập mật khẩu; chưa đăng ký mặt vẫn phải đăng nhập được.
2. Lấy JWT và phiên ENROLL từ Core, dùng `/demo` đăng ký mặt.
3. Kiểm tra hồ sơ được lưu đúng Admin và hàng đợi Telegram có FACE_ENROLLED.
4. Tạo phiên LOGIN, thực hiện liveness, kiểm tra Core nhận đúng `adminId`.
5. Nhận JWT qua Core; đăng nhập mặt thành công không tạo sự kiện ảnh.
6. Thử người chưa đăng ký, đổi người giữa thử thách, nhiều mặt, sai hướng
   và hết hạn. LOGIN không đạt mới tạo sự kiện ảnh khi có ảnh hợp lệ.
7. Thử tài khoản khóa/mất quyền; Core phải từ chối dù nhận ra khuôn mặt.
8. Thử ngắt mạng Core/Telegram và gửi lại request; đối chiếu trạng thái cũ,
   không tạo lại thử thách hoặc hồ sơ ngoài ý muốn.

Việc thử thật phải do nhóm thực hiện với camera/tài khoản được phép sử dụng.
Hiện chưa thử webcam, đăng ký người thật hoặc gửi Telegram thật. Đã cấp và
xác thực JWT qua Core Java chạy HTTP trong kiểm tra bridge với analyzer giả.
Kiểm thử AI đã có chỉ xác nhận chạy inference trên CPU với
tensor và ảnh trống; không chứng minh độ chính xác hay chống giả mạo.
