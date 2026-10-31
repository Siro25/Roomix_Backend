"""Ước lượng hướng quay từ 5 landmarks MTCNN, không cần tải mô hình khác."""
import math
from src.face_auth.ai.errors import FaceRejected


def classify_pose(points):
    """Chiếu vị trí mũi lên trục hai mắt để hạn chế sai số khi đầu nghiêng."""
    if points is None or len(points) != 5:
        raise FaceRejected("INVALID_LANDMARKS")
    if any(len(point) != 2 or not all(math.isfinite(float(v)) for v in point) for point in points):
        raise FaceRejected("INVALID_LANDMARKS")
    left, right, nose = points[:3]
    dx, dy = float(right[0] - left[0]), float(right[1] - left[1])
    distance = math.hypot(dx, dy)
    if distance < 10 or dx <= 0:
        raise FaceRejected("INVALID_LANDMARKS")
    if abs(math.atan2(dy, dx)) > math.radians(25):
        raise FaceRejected("HEAD_TOO_TILTED")
    middle_x = (float(left[0]) + float(right[0])) / 2
    middle_y = (float(left[1]) + float(right[1])) / 2
    yaw = ((float(nose[0]) - middle_x) * dx + (float(nose[1]) - middle_y) * dy) / distance ** 2
    if yaw < -0.22:
        return "TURN_LEFT"
    if yaw > 0.22:
        return "TURN_RIGHT"
    return "CENTER" if abs(yaw) < 0.12 else "TRANSITION"
