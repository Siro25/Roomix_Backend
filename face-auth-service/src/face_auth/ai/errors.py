"""Phân biệt ảnh không đạt với dịch vụ AI đang lỗi."""


class FaceRejected(Exception):
    """Ảnh hoặc thử thách không đạt yêu cầu xác thực."""

    def __init__(self, reason, photo=None):
        super().__init__(reason)
        self.photo = photo


class FaceUnavailable(Exception):
    """Lỗi xử lý AI, không được coi là người dùng nhận dạng sai."""
