# Hợp đồng Face Python - Core Java

Tài liệu này mô tả các API tích hợp Face đã được triển khai trong Core Java.
Core có đăng nhập password/JWT, GET /api/users/me và các API phiên/callback/
complete dưới đây. Xem core-service/docs/FACE_AUTH.md để cấu hình token/bảng phiên.
Gateway route `/api/face/**` đến Face (cổng local 8003, cần khớp cấu hình
container/Gateway). Đăng nhập password hoạt động độc lập với Face.

## Xác thực nội bộ

Face gửi `X-Service-Token` ở tất cả request nội bộ. Core kiểm tra token bằng
giá trị bí mật được thống nhất với `FACE_SERVICE_TOKEN`. Chỉ cho phép mạng
nội bộ hoặc HTTPS giữa service. Không cho trình duyệt gọi API nội bộ hay
tin vào header `adminId` do client tự khai.

## 1. Core tạo phiên cho trình duyệt

Đường dẫn công khai trong checkout hiện tại được thống nhất như sau. Phiên có:

Frontend đăng ký mặt dùng `POST /api/auth/face/sessions`,
header `Authorization: Bearer ...`, body `{"purpose": "ENROLL"}`.
Response trực tiếp hoặc trong `data`:
`{"authSessionId": "...", "purpose": "ENROLL", "expiresAt": 1792742400.0}`.
Đường dẫn/DTO này đã có trong Core Java của checkout hiện tại. Khi merge vào
Core của thành viên khác, đối chiếu SecurityFilterChain/schema/DTO trước.
ID Admin luôn lấy từ phiên/JWT đã xác thực, không lấy từ body trình duyệt.

- `authSessionId`: mã ngẫu nhiên khó đoán, ít nhất 128 bit entropy.
- `purpose`: LOGIN hoặc ENROLL, không được đổi mục đích.
- `expiresAt`: Unix seconds UTC, đề xuất tối đa 90 giây.
- Giá trị thời hạn phải hữu hạn. `adminId` ENROLL nhận UUID chuẩn dạng chuỗi
  chữ thường có dấu gạch nối, khớp kiểu ID đang dùng trong frontend.
  ID số nguyên dương vừa Java Long vẫn được hỗ trợ cho dữ liệu Face cũ.
- Trạng thái: đang chờ, đã hoàn tất, thất bại hoặc hết hạn.
- ENROLL: ràng buộc Admin đã đăng nhập bằng password; lưu `adminId` từ JWT.
- LOGIN: chưa biết Admin nào, không bắt buộc có JWT người dùng.

Frontend nhận phiên và dùng nó để tạo challenge bên Face. Core phải ràng
buộc phiên với trình duyệt đã tạo bằng cookie hoặc một bí mật riêng để một
client khác không chỉ dựa vào authSessionId mà lấy được JWT sau khi hoàn tất.

Frontend LOGIN dùng cùng `POST /api/auth/face/sessions`, body
`{"purpose":"LOGIN"}`, không yêu cầu JWT. Core trả thêm `completionToken`
ngẫu nhiên mật mã (đề xuất 32 byte, base64url/hex) cho trình duyệt tạo phiên.
Frontend giữ bí mật này trong bộ nhớ, không gửi đến Face.

Sau khi Face SUCCEEDED, frontend gọi `POST /api/auth/face/complete`
với `{"authSessionId":"...","completionToken":"..."}` để nhận JWT từ Core.
Core chỉ chấp nhận sau callback Face hợp lệ và kiểm tra lại quyền/tài khoản
khóa. Không tin adminId/verified do trình duyệt tự khai; chỉ authSessionId
không đủ để nhận JWT. API complete cần idempotent cho cùng phiên/bí mật khi
retry; không tạo thêm phiên đăng nhập/thông báo. Mã bí mật không xuất hiện
trong API trạng thái Face hoặc validate nội bộ. Không log bí mật trong body.

Các API này đã có trong Java. Complete chỉ retry khi phiên 90 giây còn hạn;
trả cùng JWT với cấu hình khóa/issuer không đổi và expiresIn là thời gian còn lại.

## 2. Face xin phép bắt đầu

`POST /internal/face/sessions/validate`

```json
{"authSessionId": "...", "purpose": "ENROLL"}
```

Với ENROLL, Face chuyển nguyên `Authorization: Bearer ...` đến Core.
Core kiểm tra JWT, phương thức đăng nhập password, quyền ADMIN, trạng thái
khóa, phiên còn hạn/chưa hoàn tất và đúng chủ sở hữu. `adminId` chỉ do Core trả.
Với LOGIN, không yêu cầu JWT nhưng kiểm tra phiên LOGIN hợp lệ.

Chấp nhận:

```json
{"accepted": true, "adminId": "10000000-0000-4000-8000-000000000005", "expiresAt": 1792742400.0}
```

LOGIN có thể trả `adminId: null`. Từ chối: `{"accepted": false}` hoặc HTTP
401/403/404/409/410. Các lỗi 5xx/network/JSON sai được Face coi là Core tạm
không khả dụng; tuyệt đối không chuyển sang chế độ tự xác thực.

Nếu phiên hết hạn trong lúc Face đang chạy inference, Face trả 410 và không
gửi kết quả đó đến Core. Lỗi runtime của mô hình trả 503 AI_UNAVAILABLE,
không được coi là verified=false và không tạo thông báo nhận dạng sai.

## 3. Face gửi kết quả

`POST /internal/face/results`

```json
{
  "authSessionId": "...",
  "purpose": "LOGIN",
  "verified": true,
  "adminId": "10000000-0000-4000-8000-000000000005",
  "reason": null
}
```

Khi thất bại, `verified: false`, `adminId: null`, `reason` là mã lỗi như
`FACE_NOT_RECOGNIZED`, `MULTIPLE_FACES`, `FACE_CHANGED`, `AMBIGUOUS_FACE`.

Core kiểm tra lại token nội bộ, phiên, thời hạn và mục đích. Khi thành công:

- LOGIN: kiểm tra Admin còn quyền ADMIN và không bị khóa, hoàn tất phiên.
  JWT chỉ do Core cấp qua luồng nhận kết quả đã ràng buộc với trình duyệt.
- ENROLL: kiểm tra Admin đúng chủ sở hữu phiên và vẫn đủ quyền. Phản hồi
  chấp nhận cho phép Face lưu mẫu. Frontend coi **response cuối từ Face** là
  xác nhận đã lưu thành công; phản hồi Core chưa phải bằng chứng Face đã lưu.
- Khi `verified: false`, ghi nhận lần thử thất bại và kết thúc phiên.

Response:

```json
{"accepted": true}
```

`accepted` là Core chấp nhận xử lý kết quả; Face chỉ trả SUCCEEDED khi cả
`verified` và `accepted` đều true. Không trả JWT cho Face.

Face cung cấp `GET /api/face/challenges/{challengeId}` để frontend đọc lại
kết quả khi mất response cuối. API này không cấp JWT hoặc gọi Core lại.
Frontend chính vẫn hoàn tất phiên qua Core để nhận JWT. Xem công cụ thử
camera trong `CAMERA_DEMO.md`.

**Bắt buộc idempotent theo authSessionId:** nếu timeout sau khi Core đã xử
lý, Face sẽ gửi lại cùng kết quả. Core trả lại quyết định ban đầu cho đúng
payload, không cấp thêm JWT hoặc ghi thêm lần thử. Nếu payload khác lần đầu,
từ chối. Kết quả hết hạn không được dùng để tạo phiên đăng nhập mới.

Không có transaction phân tán giữa Core và Face. Nếu Core chấp nhận ENROLL
nhưng Face lỗi lưu DB, frontend chưa được báo thành công; retry đúng kết
quả idempotent hoặc tạo lại phiên sau khi đối soát.

## Telegram

Face ghi hàng đợi ảnh ở hai sự kiện: LOGIN thất bại sau quyết định Core,
ENROLL thành công sau lưu DB. Worker gửi riêng và có retry (xem TELEGRAM.md). Core không gửi lại ảnh cho cùng sự kiện để tránh trùng.
Core đã có hàng đợi thông báo chữ cho password đúng/sai và LOGIN mặt thành
công, chỉ tạo sau khi cấp JWT. Retry complete không tạo thêm thông báo.
Core không gửi thêm chữ ở callback LOGIN thất bại hoặc ENROLL; các ảnh này
do Face xử lý. Xem core-service/docs/TELEGRAM.md; chưa gửi bot thật trong test.
