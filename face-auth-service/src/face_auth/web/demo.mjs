import { FaceCameraClient } from "./face_camera.mjs";

const byId = id => document.getElementById(id);
const labels = {
  CENTER: "Nhìn chính diện và giữ yên.",
  TURN_LEFT: "Đưa mũi về phía trái của khung hình và giữ một nhịp.",
  TURN_RIGHT: "Đưa mũi về phía phải của khung hình và giữ một nhịp.",
};
const errors = {
  AI_UNAVAILABLE: "Mô hình AI đang lỗi hoặc quá tải. Chưa có kết quả xác thực; hãy thử lại sau.",
  REQUEST_TIMEOUT: "Dịch vụ phản hồi quá lâu. Có thể đọc lại kết quả trước khi thử phiên mới.",
  INVALID_SERVER_RESPONSE: "Dịch vụ trả dữ liệu không hợp lệ. Kiểm tra cấu hình Core/Gateway.",
  ADMIN_LOGIN_REQUIRED: "Cần JWT Admin đã đăng nhập bằng mật khẩu để đăng ký mặt.",
  CORE_SESSION_REJECTED: "Core từ chối phiên. Kiểm tra mục đích, JWT và quyền Admin.",
  CORE_SESSION_EXPIRED: "Phiên Core đã hết hạn. Hãy tạo phiên mới.",
  SESSION_ALREADY_STARTED: "Phiên này đã được dùng. Hãy lấy phiên mới từ Core.",
  CHALLENGE_EXPIRED: "Thử thách đã hết hạn. Hãy lấy phiên mới từ Core.",
  CORE_UNAVAILABLE: "Chưa kết nối được Core hoặc phản hồi Core không hợp lệ.",
  DATABASE_UNAVAILABLE: "Chưa kết nối được PostgreSQL hoặc database đang bận.",
  NO_FACE: "Không phát hiện khuôn mặt.", MULTIPLE_FACES: "Chỉ để một người trong khung hình.",
  LOW_FACE_QUALITY: "Mặt quá nhỏ, thiếu sáng hoặc ảnh chưa rõ.", FACE_TOO_BLURRY: "Ảnh mặt bị mờ.",
  FACE_OUTSIDE_IMAGE: "Đưa toàn bộ khuôn mặt vào khung hình.", HEAD_TOO_TILTED: "Giữ đầu thẳng, tránh nghiêng đầu.",
  FACE_NOT_RECOGNIZED: "Không khớp hồ sơ Admin đã đăng ký.", FACE_CHANGED: "Khuôn mặt thay đổi trong lúc thực hiện.",
  FACE_ALREADY_ENROLLED: "Khuôn mặt đã được đăng ký cho Admin khác.", AMBIGUOUS_FACE: "Kết quả gần với nhiều hồ sơ. Cần thử lại.",
  LIVENESS_TIMEOUT: "Thực hiện một bước quá 12 giây. Hãy tạo phiên mới và thử lại.",
  LIVENESS_FAILED: "Chưa hoàn thành thử thách người thật.", CORE_REJECTED: "Core không chấp nhận đăng nhập hoặc đăng ký.",
  CAMERA_UNAVAILABLE: "Không có API camera. Hãy mở bằng localhost hoặc HTTPS.",
  CAMERA_NOT_READY: "Camera chưa có hình. Hãy thử lại.",
};
function message(text, error = false) {
  byId("status").textContent = text;
  byId("status").dataset.kind = error ? "error" : "info";
}
function showResult(result, purpose) {
  if (result.status === "FAILED") message(errors[result.reason] || "Xác thực thất bại. Hãy tạo phiên mới.", true);
  else if (result.status === "SUCCEEDED") message(purpose === "ENROLL"
    ? "Đã đăng ký khuôn mặt thành công."
    : "Face và Core đã chấp nhận kết quả. Hoàn tất nhận JWT qua Core để vào trang quản trị.");
  else message(result.reason === "CORE_PENDING" ? "Kết quả đang chờ Core."
    : `Phiên đang xử lý. ${labels[result.action] || ""}`);
}
let lastPurpose = "LOGIN";
const client = new FaceCameraClient({
  video: byId("camera"), canvas: byId("capture"),
  onUpdate(update) {
    if (update.phase === "OPENING_CAMERA") message("Đang xin quyền camera…");
    if (update.phase === "CAPTURING") message((update.reason === "FACE_TOO_BLURRY"
      ? "Ảnh đang mờ. Giữ đầu yên để camera lấy nét. " : "") + (labels[update.action] || "Đang chờ kết quả…"));
    if (update.phase === "RETRYING") message("Dịch vụ đang bận. Đang thử lại cùng yêu cầu…");
  },
});
function busy(value) {
  ["purpose", "sessionId", "token", "start"].forEach(id => { byId(id).disabled = value; });
  byId("stop").disabled = !value;
  byId("checkStatus").disabled = value || !client.challengeId;
}
byId("purpose").addEventListener("change", () => {
  const enroll = byId("purpose").value === "ENROLL";
  byId("tokenField").hidden = !enroll;
  byId("token").required = enroll;
  if (!enroll) byId("token").value = "";
});
byId("sessionForm").addEventListener("submit", async event => {
  event.preventDefault();
  lastPurpose = byId("purpose").value;
  const token = byId("token").value;
  byId("token").value = "";
  busy(true);
  try {
    showResult(await client.start({ authSessionId: byId("sessionId").value.trim(), purpose: lastPurpose, token }), lastPurpose);
  } catch (error) {
    if (error.name === "AbortError") message("Đã dừng camera. Phiên chưa hoàn tất sẽ hết hạn.");
    else if (error.name === "NotAllowedError") message("Bạn chưa cấp quyền camera. Chưa tạo thử thách Face.", true);
    else message(errors[error.code] || "Không thể hoàn tất. Kiểm tra camera/kết nối hoặc đọc lại kết quả.", true);
  } finally { busy(false); }
});
byId("stop").addEventListener("click", () => client.stop());
byId("checkStatus").addEventListener("click", async () => {
  byId("checkStatus").disabled = true;
  try { showResult(await client.readStatus(), lastPurpose); }
  catch (error) { message(errors[error.code] || "Không đọc được kết quả phiên.", true); }
  finally { byId("checkStatus").disabled = false; }
});
window.addEventListener("pagehide", () => client.stop());
