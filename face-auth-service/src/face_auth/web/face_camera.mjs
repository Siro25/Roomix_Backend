// Client camera dùng lại được: không tạo tài khoản, phiên Core hoặc JWT.
export class FaceApiError extends Error {
  constructor(code, status = 0) {
    super(code);
    this.code = code;
    this.status = status;
  }
}

export function delay(milliseconds, signal) {
  return new Promise((resolve, reject) => {
    const abort = () => {
      clearTimeout(timer);
      reject(new DOMException("Stopped", "AbortError"));
    };
    const timer = setTimeout(() => {
      signal.removeEventListener("abort", abort);
      resolve();
    }, milliseconds);
    signal.addEventListener("abort", abort, { once: true });
    if (signal.aborted) abort();
  });
}

export class FaceCameraClient {
  constructor({ video, canvas, onUpdate = () => {}, fetchFn = globalThis.fetch.bind(globalThis),
    mediaDevices = globalThis.navigator?.mediaDevices, wait = delay, now = Date.now, requestTimeoutMs = 20000 }) {
    Object.assign(this, { video, canvas, onUpdate, fetchFn, mediaDevices, wait, now, requestTimeoutMs });
    this.running = false;
    this.challengeId = null;
  }

  async request(url, options, signal) {
    const controller = new AbortController();
    const abort = () => controller.abort(signal.reason);
    signal?.addEventListener("abort", abort, { once: true });
    if (signal?.aborted) abort();
    const timer = setTimeout(() => controller.abort(new DOMException("Request timed out", "TimeoutError")), this.requestTimeoutMs);
    try {
      const response = await this.fetchFn(url, { ...options, signal: controller.signal, cache: "no-store", credentials: "omit" });
      let body;
      try { body = await response.json(); }
      catch (error) {
        if (controller.signal.aborted) throw error;
        throw new FaceApiError("INVALID_SERVER_RESPONSE", response.status);
      }
      if (!body || typeof body !== "object" || Array.isArray(body)) throw new FaceApiError("INVALID_SERVER_RESPONSE", response.status);
      if (!response.ok) {
        throw new FaceApiError(typeof body.detail === "string" ? body.detail : "INVALID_REQUEST", response.status);
      }
      return body;
    } catch (error) {
      if (signal?.aborted) throw new DOMException("Stopped", "AbortError");
      if (controller.signal.reason?.name === "TimeoutError") throw new FaceApiError("REQUEST_TIMEOUT");
      throw error;
    } finally {
      clearTimeout(timer);
      signal?.removeEventListener("abort", abort);
    }
  }

  stop() {
    this.controller?.abort();
    this.releaseCamera();
  }

  releaseCamera() {
    this.stream?.getTracks().forEach(track => track.stop());
    this.stream = null;
    this.video.srcObject = null;
  }

  async capture() {
    const width = this.video.videoWidth;
    const height = this.video.videoHeight;
    if (!width || !height) throw new FaceApiError("CAMERA_NOT_READY");
    const scale = Math.min(960 / width, 720 / height, 1);
    this.canvas.width = Math.round(width * scale);
    this.canvas.height = Math.round(height * scale);
    // Không lật ảnh: head_pose.py tính hướng dựa trên ảnh gốc.
    this.canvas.getContext("2d").drawImage(this.video, 0, 0, this.canvas.width, this.canvas.height);
    const photo = await new Promise(resolve => this.canvas.toBlob(resolve, "image/jpeg", 0.8));
    if (!photo || photo.size > 1024 * 1024) throw new FaceApiError("INVALID_IMAGE_SIZE");
    return photo;
  }

  async readStatus(signal) {
    if (!this.challengeId) throw new FaceApiError("NO_CHALLENGE");
    return this.validateResult(await this.request(`/api/face/challenges/${encodeURIComponent(this.challengeId)}`, {}, signal));
  }

  validateResult(result) {
    if (!["IN_PROGRESS", "SUCCEEDED", "FAILED"].includes(result.status)) {
      throw new FaceApiError("INVALID_SERVER_RESPONSE");
    }
    return result;
  }

  async sendFrame(photo, signal) {
    for (let attempt = 0; attempt < 3; attempt++) {
      try {
        return this.validateResult(await this.request(`/api/face/challenges/${encodeURIComponent(this.challengeId)}/frames`, {
          method: "POST", headers: { "Content-Type": "image/jpeg" }, body: photo,
        }, signal));
      } catch (error) {
        if (signal.aborted) throw error;
        if (["CHALLENGE_ALREADY_USED", "REQUEST_TIMEOUT"].includes(error.code) || !(error instanceof FaceApiError)) {
          // Response cuối có thể mất khi server đã hoàn tất. Đọc lại, không nhận mặt lại.
          const result = await this.readStatus(signal);
          if (result.status !== "IN_PROGRESS") return result;
          throw error;
        }
        if (attempt === 2 || ![503, 429].includes(error.status)) throw error;
        this.onUpdate({ phase: "RETRYING", reason: error.code });
        await this.wait(error.status === 429 ? 350 : 1000, signal);
      }
    }
  }

  async start({ authSessionId, purpose, token = "" }) {
    if (this.running) throw new FaceApiError("ALREADY_RUNNING");
    if (!authSessionId || !["LOGIN", "ENROLL"].includes(purpose)) throw new FaceApiError("INVALID_REQUEST");
    if (purpose === "ENROLL" && !token.trim()) throw new FaceApiError("ADMIN_LOGIN_REQUIRED");
    if (!this.mediaDevices?.getUserMedia) throw new FaceApiError("CAMERA_UNAVAILABLE");
    this.running = true;
    this.challengeId = null;
    this.controller = new AbortController();
    const signal = this.controller.signal;
    try {
      this.onUpdate({ phase: "OPENING_CAMERA" });
      // Xin camera trước, tránh tiêu thụ phiên Face nếu người dùng từ chối quyền.
      const stream = await this.mediaDevices.getUserMedia({ video: { width: 640, height: 480 }, audio: false });
      if (signal.aborted) {
        stream.getTracks().forEach(track => track.stop());
        throw new DOMException("Stopped", "AbortError");
      }
      this.stream = stream;
      this.video.srcObject = stream;
      await this.video.play();
      const headers = { "Content-Type": "application/json" };
      if (purpose === "ENROLL") headers.Authorization = `Bearer ${token.trim().replace(/^Bearer\s+/i, "")}`;
      const challenge = await this.request("/api/face/challenges", {
        method: "POST", headers, body: JSON.stringify({ authSessionId, purpose }),
      }, signal);
      if (typeof challenge.challengeId !== "string" || !challenge.challengeId ||
          !Array.isArray(challenge.actions) || !challenge.actions.length ||
          !challenge.actions.every(action => ["CENTER", "TURN_LEFT", "TURN_RIGHT"].includes(action)) ||
          !Number.isFinite(challenge.expiresAt)) {
        throw new FaceApiError("INVALID_SERVER_RESPONSE");
      }
      this.challengeId = challenge.challengeId;
      let action = challenge.actions[0];
      let feedback;
      while (!signal.aborted) {
        if (this.now() >= challenge.expiresAt * 1000) throw new FaceApiError("CHALLENGE_EXPIRED", 410);
        this.onUpdate({ phase: "CAPTURING", action, reason: feedback });
        const result = await this.sendFrame(await this.capture(), signal);
        if (result.status !== "IN_PROGRESS") return result;
        action = result.action;
        feedback = result.reason;
        await this.wait(350, signal);
      }
      throw new DOMException("Stopped", "AbortError");
    } finally {
      this.releaseCamera();
      this.running = false;
    }
  }
}
