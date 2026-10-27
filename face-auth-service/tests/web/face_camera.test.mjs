import test from "node:test";
import assert from "node:assert/strict";
import { FaceCameraClient } from "../../src/face_auth/web/face_camera.mjs";

function setup(responses) {
  const calls = [];
  let stopped = 0;
  const track = { stop() { stopped++; } };
  const stream = { getTracks: () => [track] };
  const video = { videoWidth: 640, videoHeight: 480, srcObject: null, async play() {} };
  const canvas = { getContext: () => ({ drawImage() {} }), toBlob: callback => callback(new Blob(["jpeg"])) };
  const updates = [];
  const client = new FaceCameraClient({ video, canvas, onUpdate: value => updates.push(value),
    mediaDevices: { async getUserMedia(constraints) { assert.equal(constraints.audio, false); return stream; } },
    fetchFn: async (url, options) => {
      calls.push({ url, options });
      const item = responses.shift();
      if (item instanceof Error) throw item;
      assert.ok(item, "Unexpected extra request");
      return { ok: (item.code || 200) < 400, status: item.code || 200, json: async () => item.body };
    }, wait: async () => {}, now: () => 1000,
  });
  return { client, calls, updates, video, canvas, stream, stopped: () => stopped };
}
const challenge = { body: { challengeId: "challenge", actions: ["CENTER"], expiresAt: 100 } };
const input = { authSessionId: "core-session-id-12345", purpose: "LOGIN" };

test("blurred transition frame keeps the camera open and reports recovery feedback", async () => {
  const app = setup([challenge,
    { body: { status: "IN_PROGRESS", action: "CENTER", reason: "FACE_TOO_BLURRY" } },
    { body: { status: "SUCCEEDED" } }]);
  const result = await app.client.start(input);
  assert.equal(result.status, "SUCCEEDED");
  assert.ok(app.updates.some(update => update.phase === "CAPTURING" && update.reason === "FACE_TOO_BLURRY"));
  assert.equal(app.stopped(), 1);
});

test("login sends sequential frames without JWT and closes camera", async () => {
  const app = setup([challenge, { body: { status: "IN_PROGRESS", action: "TURN_LEFT" } },
    { body: { status: "SUCCEEDED" } }]);
  const result = await app.client.start(input);
  assert.equal(result.status, "SUCCEEDED");
  assert.equal(app.calls.length, 3);
  assert.equal(app.calls[0].options.headers.Authorization, undefined);
  assert.equal(app.calls[1].options.headers["Content-Type"], "image/jpeg");
  assert.equal(app.stopped(), 1);
  assert.equal(app.video.srcObject, null);
  assert.equal(app.client.running, false);
});

test("enrollment forwards JWT only to challenge creation", async () => {
  const app = setup([challenge, { body: { status: "SUCCEEDED" } }]);
  await app.client.start({ ...input, purpose: "ENROLL", token: "Bearer admin-jwt" });
  assert.equal(app.calls[0].options.headers.Authorization, "Bearer admin-jwt");
  assert.equal(app.calls[1].options.headers.Authorization, undefined);
  assert.deepEqual(JSON.parse(app.calls[0].options.body), { authSessionId: input.authSessionId, purpose: "ENROLL" });
});

test("enrollment without JWT does not open camera or send requests", async () => {
  const app = setup([]);
  await assert.rejects(app.client.start({ ...input, purpose: "ENROLL" }), /ADMIN_LOGIN_REQUIRED/);
  assert.equal(app.calls.length, 0);
  assert.equal(app.stopped(), 0);
});

test("camera denial does not consume a Face challenge", async () => {
  const app = setup([]);
  app.client.mediaDevices.getUserMedia = async () => { throw new DOMException("Denied", "NotAllowedError"); };
  await assert.rejects(app.client.start(input), { name: "NotAllowedError" });
  assert.equal(app.calls.length, 0);
  assert.equal(app.client.running, false);
});

test("503 retries the same photo and eventually stops", async () => {
  const app = setup([challenge, { code: 503, body: { detail: "CORE_UNAVAILABLE" } },
    { body: { status: "FAILED", reason: "FACE_NOT_RECOGNIZED" } }]);
  const result = await app.client.start(input);
  assert.equal(result.status, "FAILED");
  assert.equal(app.calls[1].options.body, app.calls[2].options.body);
  assert.equal(app.stopped(), 1);
});

test("lost final response is recovered through status, without a new frame", async () => {
  const app = setup([challenge, new TypeError("Network error"), { body: { status: "SUCCEEDED" } }]);
  assert.equal((await app.client.start(input)).status, "SUCCEEDED");
  assert.equal(app.calls[2].url, "/api/face/challenges/challenge");
  assert.equal(app.calls[2].options.method, undefined);
});

test("a completed challenge returns its result without repeating recognition", async () => {
  const app = setup([challenge, { code: 409, body: { detail: "CHALLENGE_ALREADY_USED" } },
    { body: { status: "FAILED", reason: "CORE_REJECTED" } }]);
  assert.equal((await app.client.start(input)).reason, "CORE_REJECTED");
  assert.equal(app.calls.length, 3);
});

test("expiration closes camera without submitting a frame", async () => {
  const app = setup([{ body: { ...challenge.body, expiresAt: 0.5 } }]);
  await assert.rejects(app.client.start(input), /CHALLENGE_EXPIRED/);
  assert.equal(app.calls.length, 1);
  assert.equal(app.stopped(), 1);
});

test("stop while waiting for camera releases a late permission grant", async () => {
  const app = setup([]);
  let grant;
  app.client.mediaDevices.getUserMedia = () => new Promise(resolve => { grant = resolve; });
  const running = app.client.start(input);
  app.client.stop();
  grant(app.stream);
  await assert.rejects(running, { name: "AbortError" });
  assert.equal(app.stopped(), 1);
  assert.equal(app.calls.length, 0);
});

test("repeated 503 does not retry forever", async () => {
  const error = { code: 503, body: { detail: "CORE_UNAVAILABLE" } };
  const app = setup([challenge, error, error, error]);
  await assert.rejects(app.client.start(input), /CORE_UNAVAILABLE/);
  assert.equal(app.calls.length, 4);
  assert.equal(app.stopped(), 1);
});

test("a timed out final request can recover a committed result", async () => {
  const app = setup([challenge, { body: { status: "SUCCEEDED" } }]);
  const originalFetch = app.client.fetchFn;
  app.client.requestTimeoutMs = 5;
  app.client.fetchFn = (url, options) => url.endsWith("/frames")
    ? new Promise((resolve, reject) => options.signal.addEventListener("abort", () => reject(options.signal.reason), { once: true }))
    : originalFetch(url, options);
  assert.equal((await app.client.start(input)).status, "SUCCEEDED");
  assert.equal(app.calls[1].url, "/api/face/challenges/challenge");
  assert.equal(app.stopped(), 1);
});

test("timed out challenge creation closes the camera", async () => {
  const app = setup([]);
  app.client.requestTimeoutMs = 5;
  app.client.fetchFn = (url, options) => new Promise((resolve, reject) => {
    options.signal.addEventListener("abort", () => reject(options.signal.reason), { once: true });
  });
  await assert.rejects(app.client.start(input), /REQUEST_TIMEOUT/);
  assert.equal(app.client.challengeId, null);
  assert.equal(app.stopped(), 1);
});

test("non JSON gateway errors do not leave the camera running", async () => {
  const app = setup([]);
  app.client.fetchFn = async () => ({ ok: false, status: 502, json: async () => { throw new SyntaxError("HTML body"); } });
  await assert.rejects(app.client.start(input), /INVALID_SERVER_RESPONSE/);
  assert.equal(app.stopped(), 1);
});

test("invalid challenge responses are rejected before posting images", async () => {
  for (const body of [{ challengeId: "x", expiresAt: 100, actions: ["UNKNOWN"] },
                     { challengeId: "x", expiresAt: "never", actions: ["CENTER"] },
                     { status: "OK" }]) {
    const app = setup([{ body }]);
    await assert.rejects(app.client.start(input), /INVALID_SERVER_RESPONSE/);
    assert.equal(app.calls.length, 1);
    assert.equal(app.stopped(), 1);
  }
});

test("unknown face result statuses are rejected", async () => {
  const app = setup([challenge, { body: { status: "MAGIC_SUCCESS" } }]);
  await assert.rejects(app.client.start(input), /INVALID_SERVER_RESPONSE/);
  assert.equal(app.stopped(), 1);
});
