"""Giải mã và giới hạn ảnh camera trước khi đưa vào pipeline AI cũ."""
import io
from PIL import Image, ImageOps, UnidentifiedImageError
from src.face_auth.ai.errors import FaceRejected

MAX_IMAGE_BYTES = 1024 * 1024
MAX_IMAGE_PIXELS = 1920 * 1080


def decode_image(content):
    """Trả ảnh RGB và JPEG đã mã hóa lại để gửi Telegram nếu cần."""
    if not content or len(content) > MAX_IMAGE_BYTES:
        raise FaceRejected("INVALID_IMAGE_SIZE")
    try:
        with Image.open(io.BytesIO(content)) as source:
            if source.format not in ("JPEG", "PNG", "WEBP") or getattr(source, "is_animated", False):
                raise FaceRejected("UNSUPPORTED_IMAGE")
            if source.width * source.height > MAX_IMAGE_PIXELS:
                raise FaceRejected("IMAGE_TOO_LARGE")
            image = ImageOps.exif_transpose(source).convert("RGB")
    except (UnidentifiedImageError, OSError, Image.DecompressionBombError) as exc:
        raise FaceRejected("INVALID_IMAGE") from exc
    preview = image.copy()
    preview.thumbnail((960, 720))
    output = io.BytesIO()
    preview.save(output, format="JPEG", quality=80)
    return image, output.getvalue()
