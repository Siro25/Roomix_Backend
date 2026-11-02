# Face capture improvements

The public challenge routes, Core callbacks, JSON fields, session IDs,
environment variable names and database schema are unchanged.

- Challenge frames use full-image MTCNN detection and landmarks. The generic
  YOLO person detector no longer vetoes valid faces or runs on every frame.
  The original YOLO model/helper is retained for compatibility.
- Blur is scored on a 160 x 160 grayscale face crop, without sharpening.
  The numerical threshold remains 40; it requires validation with real cameras.
- Temporary capture/landmark errors return existing `IN_PROGRESS` feedback.
  They reset consecutive hits but do not reset the step deadline, extend the
  session or bypass the frame budget. Multiple faces and identity changes fail.
- FaceNet still runs on accepted frames to verify identity continuity. It is
  deliberately not skipped on head turns without a validated replacement.
- The browser requests ideal 1280 x 720 video (not a mandatory constraint),
  retains the existing maximum upload dimensions/bytes, uses JPEG quality 0.9,
  and allows 500 ms for exposure/focus before creating the Face challenge.
- DEBUG logging in `src.face_auth.ai.face_service` records crop dimensions,
  sharpness, detection time and total frame time. It never logs images,
  embeddings, admin IDs or tokens. Detection time includes inference-lock wait.

Existing enrolled profiles remain compatible because FaceNet extraction and
embedding normalization are unchanged. No re-enrollment is required by this
change. Core remains the only issuer of JWTs; Telegram event rules are unchanged.

After deploying, compare enrollment/login latency and rejection rates using
real webcams, different light levels, glasses and head turns. Check that still
photos cannot complete the movement sequence. Movement liveness remains an MVP,
not a dedicated presentation-attack detection model. Do not claim a speedup or
change matching/blur thresholds until measurements support it.

Docker users must rebuild/recreate the Face service with their existing Compose
project, env file and overrides; restarting an old container does not load code.
