"""ID thuộc Core: UUID cho tích hợp mới, giữ ID số của dữ liệu Face cũ."""
from uuid import UUID


def validate_admin_id(value):
    if type(value) is int and 0 < value <= 2 ** 63 - 1:
        return value
    if isinstance(value, str):
        try:
            parsed = UUID(value)
        except ValueError:
            pass
        else:
            # Chỉ nhận dạng chuẩn có dấu gạch nối, không tự sửa ID từ Core.
            if parsed.int != 0 and str(parsed) == value:
                return value
    raise ValueError("INVALID_ADMIN_ID")


def stored_admin_id(value):
    """TEXT trong PostgreSQL không làm thay đổi kiểu ID khi gửi về Core."""
    if value.isascii() and value.isdecimal():
        value = int(value)
    return validate_admin_id(value)
