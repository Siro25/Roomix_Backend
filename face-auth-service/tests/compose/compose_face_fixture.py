"""Only mounted by the Compose integration test; never copied into the image."""
from types import SimpleNamespace
from src.face_auth.ai.errors import FaceRejected
from src.face_auth.main import create_app


class FixtureAnalyzer:
    def analyze(self, content):
        identity, pose = content.decode("ascii").split(":", 1)
        if identity not in ("FIRST", "OTHER") or pose not in ("CENTER", "TURN_LEFT", "TURN_RIGHT"):
            raise FaceRejected("NO_FACE")
        vector = [0.0] * 512
        vector[0 if identity == "FIRST" else 1] = 1.0
        return SimpleNamespace(embedding=vector, pose=pose, photo=b"test-only-photo")


app = create_app(analyzer=FixtureAnalyzer())
