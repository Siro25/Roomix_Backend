"""Kiểm tra môi trường/AI thật; không mở camera, lưu hồ sơ hay gọi Core."""
import argparse
import importlib.metadata
import json
from pathlib import Path
import sys

PACKAGES = (
    "fastapi", "uvicorn", "httpx", "psycopg", "numpy", "Pillow",
    "opencv-python", "torch", "torchvision", "facenet-pytorch", "ultralytics",
)


def installed_versions():
    result = {}
    for package in PACKAGES:
        try:
            result[package] = importlib.metadata.version(package)
        except importlib.metadata.PackageNotFoundError:
            result[package] = None
    return result


def check_models(image_path=None):
    """Chạy inference thật trên ảnh trống và tensor; không chứng minh độ chính xác."""
    import numpy as np
    from PIL import Image
    from src.face_auth.ai.face_service import FaceService
    from src.face_auth.ai.errors import FaceRejected

    service = FaceService.get_instance()
    # Tensor này không phải ảnh người; chỉ kiểm tra encoder chạy được.
    tensor = service._torch.zeros((1, 3, 160, 160), device=service.device)
    vector = service._encode_face_tensor(tensor)
    if len(vector) != 512 or not np.isclose(np.linalg.norm(vector), 1, atol=1e-5):
        raise RuntimeError("Encoder did not return a normalized 512-dimensional vector")
    blank = Image.new("RGB", (640, 480), "black")
    boxes, _ = service.mtcnn.detect(blank)
    if boxes is not None and len(boxes):
        raise RuntimeError("MTCNN detected a face in the blank test image")
    if service.detect_faces(np.zeros((480, 640, 3), dtype=np.uint8)):
        raise RuntimeError("YOLO detected a face/person in the blank test image")
    result = {"device": str(service.device), "embeddingDimensions": len(vector),
              "blankImageRejected": True, "models": "OK"}
    if image_path:
        path = Path(image_path)
        if path.stat().st_size > 1024 * 1024:
            raise ValueError("Image exceeds 1 MiB")
        try:
            observation = service.analyze(path.read_bytes())
            result["image"] = {"status": "ACCEPTED", "pose": observation.pose,
                               "embeddingDimensions": len(observation.embedding)}
        except FaceRejected as exc:
            result["image"] = {"status": "REJECTED", "reason": str(exc)}
    return result


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--models", action="store_true", help="Load models and run inference; may download weights")
    parser.add_argument("--image", help="Optional local image to analyze; never saves an enrollment")
    arguments = parser.parse_args(argv)
    versions = installed_versions()
    report = {"python": sys.version.split()[0], "packages": versions}
    missing = [name for name, version in versions.items() if version is None]
    if missing:
        report.update({"status": "MISSING_DEPENDENCIES", "missing": missing})
        print(json.dumps(report, indent=2))
        return 1
    try:
        if arguments.models or arguments.image:
            report["inference"] = check_models(arguments.image)
        report["status"] = "OK"
    except Exception as exc:
        # Diagnostic không in URL database, JWT hoặc vector sinh trắc.
        report.update({"status": "MODELS_FAILED", "errorType": type(exc).__name__})
        print(json.dumps(report, indent=2))
        return 1
    print(json.dumps(report, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
