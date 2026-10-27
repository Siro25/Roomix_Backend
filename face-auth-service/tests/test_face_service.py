"""Chạy xử lý ảnh thật; thay detector/encoder bằng fake để không tải trọng số."""
import io
import threading
import unittest
from contextlib import nullcontext
from unittest.mock import Mock
import cv2
import numpy as np
from PIL import Image
from src.face_auth.ai.face_service import FaceService
from src.face_auth.ai.errors import FaceRejected, FaceUnavailable
from tests.test_liveness import points


class FakeTensor:
    def __init__(self, shape=(1, 3, 160, 160)):
        self.shape = shape

    def dim(self):
        return len(self.shape)

    def __getitem__(self, index):
        return FakeTensor(self.shape[1:])

    def unsqueeze(self, dimension):
        return FakeTensor((1, *self.shape))

    def to(self, device):
        return self


class FaceServiceTests(unittest.TestCase):
    def test_model_type_comes_from_filename_not_project_folder(self):
        self.assertFalse(FaceService._is_face_model("C:/face-auth-service/models/yolov8n.pt"))
        self.assertTrue(FaceService._is_face_model("C:/face-auth-service/models/yolov8n-face.pt"))

    def test_model_runtime_error_is_not_reported_as_a_rejected_face(self):
        self.service.mtcnn.detect.side_effect = RuntimeError("private-model-error")
        with self.assertRaises(FaceUnavailable) as error:
            self.analyze()
        self.assertEqual(str(error.exception), "AI_UNAVAILABLE")

    def setUp(self):
        # Không gọi constructor: không tải YOLO, PyTorch hoặc FaceNet.
        self.service = object.__new__(FaceService)
        self.service._inference_lock = threading.Lock()
        self.service._torch = Mock(no_grad=nullcontext)
        self.service.device = "cpu"
        self.service.detect_faces = Mock(return_value=[(40, 30, 160, 160, 0.99)])
        self.service.mtcnn = Mock()
        self.boxes = np.array([[40, 30, 160, 160]], dtype=float)
        self.service.mtcnn.detect.return_value = self.boxes, np.array([0.99]), np.array([points()])
        self.service.mtcnn.extract.return_value = FakeTensor()
        self.service.facenet = Mock()
        self.service.facenet.return_value.cpu.return_value.numpy.return_value = np.ones((1, 512), dtype=np.float32)
        self.pixels = np.random.default_rng(42).integers(40, 210, (200, 200, 3), dtype=np.uint8)

    def photo(self, pixels=None):
        output = io.BytesIO()
        Image.fromarray(self.pixels if pixels is None else pixels).save(output, format="PNG")
        return output.getvalue()

    def analyze(self):
        return self.service.analyze(self.photo())

    def test_embedding_uses_exact_validated_box_without_redetection(self):
        observation = self.analyze()
        self.assertEqual(observation.pose, "CENTER")
        self.assertEqual(len(observation.embedding), 512)
        self.assertAlmostEqual(sum(v * v for v in observation.embedding), 1.0, places=5)
        self.assertTrue(observation.photo.startswith(b"\xff\xd8"))
        self.service.mtcnn.detect.assert_called_once()
        self.assertIs(self.service.mtcnn.extract.call_args.args[1], self.boxes)
        self.service.mtcnn.assert_not_called()

    def test_multiple_faces_are_rejected_before_encoder(self):
        self.service.mtcnn.detect.return_value = np.vstack([self.boxes, self.boxes]), [0.99, 0.99], None
        with self.assertRaisesRegex(FaceRejected, "MULTIPLE_FACES") as error:
            self.analyze()
        self.assertTrue(error.exception.photo.startswith(b"\xff\xd8"))
        self.service.facenet.assert_not_called()

    def test_mtcnn_object_arrays_are_converted_before_quality_checks(self):
        self.service.mtcnn.detect.return_value = (
            self.boxes.astype(object), np.array([0.99], dtype=object), np.array([points()]))
        observation = self.analyze()
        self.assertEqual(len(observation.embedding), 512)
        self.assertEqual(observation.pose, "CENTER")
        extracted_boxes = self.service.mtcnn.extract.call_args.args[1]
        self.assertEqual(extracted_boxes.dtype, np.float64)
        np.testing.assert_array_equal(extracted_boxes, self.boxes)

    def test_non_finite_object_coordinates_are_rejected_before_encoder(self):
        boxes = self.boxes.astype(object)
        boxes[0, 0] = float("nan")
        self.service.mtcnn.detect.return_value = boxes, np.array([0.99], dtype=object), np.array([points()])
        with self.assertRaisesRegex(FaceRejected, "LOW_FACE_QUALITY"):
            self.analyze()
        self.service.facenet.assert_not_called()

    def test_two_faces_are_still_rejected_when_yolo_misses(self):
        self.service.detect_faces.return_value = []
        self.service.mtcnn.detect.return_value = np.vstack([self.boxes, self.boxes]), [0.99, 0.99], None
        with self.assertRaisesRegex(FaceRejected, "MULTIPLE_FACES"):
            self.analyze()

    def test_no_face_is_rejected(self):
        self.service.mtcnn.detect.return_value = None, None, None
        with self.assertRaisesRegex(FaceRejected, "NO_FACE"):
            self.analyze()

    def test_low_confidence_is_rejected(self):
        self.service.mtcnn.detect.return_value = self.boxes, [0.5], np.array([points()])
        with self.assertRaisesRegex(FaceRejected, "LOW_FACE_QUALITY"):
            self.analyze()

    def test_missing_landmarks_are_rejected(self):
        self.service.mtcnn.detect.return_value = self.boxes, [0.99], None
        with self.assertRaisesRegex(FaceRejected, "INVALID_LANDMARKS"):
            self.analyze()

    def test_face_outside_image_is_rejected(self):
        self.service.mtcnn.detect.return_value = np.array([[-10, 30, 160, 160]]), [0.99], None
        with self.assertRaisesRegex(FaceRejected, "FACE_OUTSIDE_IMAGE"):
            self.analyze()

    def test_blank_photo_is_rejected(self):
        with self.assertRaisesRegex(FaceRejected, "LOW_FACE_QUALITY"):
            self.service.analyze(self.photo(np.full((200, 200, 3), 100, dtype=np.uint8)))

    def test_blurry_photo_is_rejected(self):
        blurred = cv2.GaussianBlur(self.pixels, (31, 31), 0)
        # Ảnh gradient vẫn có tương phản nhưng không có chi tiết rõ.
        gradient = np.tile(np.linspace(40, 210, 200, dtype=np.uint8), (200, 1))
        blurred[:] = gradient[:, :, None]
        with self.assertRaisesRegex(FaceRejected, "FACE_TOO_BLURRY"):
            self.service.analyze(self.photo(blurred))

    def test_encoder_rejects_zero_nan_and_wrong_dimension(self):
        for vector in (np.zeros(512), np.full(512, np.nan), np.ones(128)):
            self.service.facenet.return_value.cpu.return_value.numpy.return_value = vector
            with self.subTest(shape=vector.shape), self.assertRaisesRegex(FaceRejected, "INVALID_EMBEDDING"):
                self.analyze()

    def test_encoder_does_not_silently_select_first_face(self):
        with self.assertRaisesRegex(FaceRejected, "MULTIPLE_FACES"):
            self.service._encode_face_tensor(FakeTensor((2, 3, 160, 160)))


if __name__ == "__main__":
    unittest.main()
