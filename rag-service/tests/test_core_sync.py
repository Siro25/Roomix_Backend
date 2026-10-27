import httpx
import pytest
from fastapi.testclient import TestClient
from helpers import make_room

from app.config import settings
from app.core.gemini_client import gemini_client
from app.core.vector_store import vector_store
from app.main import app
from app.services import core_room_sync as sync_module
from app.services.core_room_sync import CoreSyncError, core_room_sync, room_from_core


def core_room(room_id, **overrides):
    data = {
        "roomId": room_id,
        "title": "Phòng khép kín Cầu Giấy",
        "description": "Gần ĐH Quốc Gia",
        "rentalPrice": 3500000,
        "depositAmount": 3500000,
        "area": 25,
        "maxTenants": 2,
        "addressStreet": "12 Trần Thái Tông",
        "ward": "Dịch Vọng",
        "district": "Cầu Giấy",
        "city": "Hà Nội",
        "hasPrivateBathroom": True,
        "hasAirConditioner": True,
        "hasWaterHeater": False,
        "hasBalcony": False,
        "amenitiesDescription": "Wifi, Chỗ để xe",
        "rented": False,
        "available": True,
        "imageUrls": ["http://img/1.jpg"],
        "availableFrom": "2026-11-01",
        "landlordName": "Bác Hùng",
        "contactPhone": "0912345678",
    }
    data.update(overrides)
    return data


@pytest.fixture
def fake_core(monkeypatch):
    """Serve a mutable {roomId: payload} dict like core-service's public API."""
    monkeypatch.setattr(settings, "CORE_SERVICE_URL", "http://core")
    rooms = {}

    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/api/rooms/search":
            return httpx.Response(200, json={
                "code": 1000,
                "data": {"content": list(rooms.values()), "last": True},
            })
        room_id = request.url.path.rsplit("/", 1)[1]
        if room_id in rooms and not rooms[room_id].get("_detail_404"):
            return httpx.Response(200, json={"code": 1000, "data": rooms[room_id]})
        return httpx.Response(404, json={})

    real_client = httpx.Client
    monkeypatch.setattr(
        sync_module.httpx, "Client",
        lambda **kw: real_client(transport=httpx.MockTransport(handler), **kw),
    )
    return rooms


def test_room_from_core_maps_real_fields():
    room = room_from_core(core_room("c1"), core_room("c1"))
    assert room.id == "c1" and room.price == 3_500_000 and room.deposit == 3_500_000
    assert room.address == "12 Trần Thái Tông" and room.district == "Cầu Giấy"
    assert room.amenities == ["Khép kín (WC riêng)", "Máy lạnh", "Wifi", "Chỗ để xe"]
    assert room.max_tenants == 2 and room.available_from == "2026-11-01"
    assert room.image_url == "http://img/1.jpg"
    assert room.landlord_name == "Bác Hùng" and room.contact_phone == "0912345678"


def test_sync_mirrors_core_and_only_reembeds_changes(fake_core):
    vector_store.add_rooms([make_room(id="stale-demo-room")])
    fake_core.update({"c1": core_room("c1"), "c2": core_room("c2", title="Phòng ban công")})

    first = core_room_sync.sync()
    assert (first["added"], first["removed"], first["unchanged"]) == (2, 1, 0)
    assert set(vector_store.rooms_col.get()["ids"]) == {"c1", "c2"}

    second = core_room_sync.sync()
    assert (second["added"], second["updated"], second["unchanged"]) == (0, 0, 2)

    fake_core["c2"] = core_room("c2", title="Phòng ban công", rentalPrice=4000000)
    del fake_core["c1"]  # rented / unpublished in core
    third = core_room_sync.sync()
    assert (third["updated"], third["removed"], third["unchanged"]) == (1, 1, 0)
    meta = vector_store.rooms_col.get(ids=["c2"])["metadatas"][0]
    assert meta["price"] == 4_000_000
    assert meta["landlord_name"] == "Bác Hùng"
    assert meta["contact_phone"] == "0912345678"


def test_sync_skips_rooms_whose_detail_is_gone(fake_core):
    fake_core["c1"] = core_room("c1")
    fake_core["ghost"] = core_room("ghost", _detail_404=True)
    core_room_sync.sync()
    assert vector_store.rooms_col.get()["ids"] == ["c1"]


def test_failed_fetch_never_deletes_existing_rooms(monkeypatch):
    monkeypatch.setattr(settings, "CORE_SERVICE_URL", "http://core")
    vector_store.add_rooms([make_room(id="keep-me")])

    def boom(request):
        raise httpx.ConnectError("core is down")

    real_client = httpx.Client
    monkeypatch.setattr(
        sync_module.httpx, "Client",
        lambda **kw: real_client(transport=httpx.MockTransport(boom), **kw),
    )
    with pytest.raises(CoreSyncError):
        core_room_sync.sync()
    assert vector_store.rooms_col.get()["ids"] == ["keep-me"]
    assert core_room_sync.last_status["state"] == "error"


def test_sync_requires_core_url():
    with pytest.raises(CoreSyncError):
        core_room_sync.sync()


# ---------------------------------------------------------------------------
# Chat answers must come from the synced (real) rooms
# ---------------------------------------------------------------------------
@pytest.fixture
def prompts(monkeypatch):
    captured = []
    monkeypatch.setattr(
        gemini_client, "generate_content",
        lambda prompt, system_instruction=None: captured.append(prompt) or "ok",
    )
    return captured


def seed_two_rooms():
    vector_store.add_rooms([
        make_room(id="a", title="Phòng giá rẻ Hai Bà Trưng", price=3_000_000),
        make_room(id="b", title="Phòng cao cấp Cầu Giấy", district="Cầu Giấy", ward="Dịch Vọng", price=6_000_000),
    ])


def test_chat_applies_constraints_written_in_the_question(prompts):
    seed_two_rooms()
    client = TestClient(app)
    body = client.post("/api/v1/chat", json={"message": "phòng dưới 4 triệu ở Hai Bà Trưng"}).json()
    assert [r["id"] for r in body["matched_rooms"]] == ["a"]
    assert "đều khớp tiêu chí" in prompts[0]
    assert "Phòng giá rẻ Hai Bà Trưng" in prompts[0] and "Phòng cao cấp" not in prompts[0]


def test_chat_is_honest_when_nothing_matches_exactly(prompts):
    seed_two_rooms()
    client = TestClient(app)
    body = client.post("/api/v1/chat", json={"message": "phòng dưới 4 triệu ở Cầu Giấy"}).json()
    assert [r["id"] for r in body["matched_rooms"]] == ["b"]  # nearest, same district
    assert "KHÔNG có phòng nào khớp đúng tiêu chí" in prompts[0]


def test_chat_with_empty_index_does_not_invent_rooms(prompts):
    client = TestClient(app)
    body = client.post("/api/v1/chat", json={"message": "tìm phòng"}).json()
    assert body["matched_rooms"] == []
    assert "không có phòng nào đang cho thuê" in prompts[0]


def test_manual_sync_endpoints(fake_core):
    fake_core["c1"] = core_room("c1")
    client = TestClient(app)
    assert client.post("/api/v1/ingest/sync").json()["added"] == 1
    status = client.get("/api/v1/ingest/sync/status").json()
    assert status["enabled"] is True and status["state"] == "ok"


def test_manual_sync_without_core_url_is_409():
    assert TestClient(app).post("/api/v1/ingest/sync").status_code == 409
