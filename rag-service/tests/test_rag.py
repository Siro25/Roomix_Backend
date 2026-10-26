from fastapi.testclient import TestClient
from helpers import make_room

from app.core.gemini_client import local_embedding
from app.core.vector_store import vector_store
from app.main import app


def test_format_room_text_omits_blank_landlord_info():
    text = vector_store.format_room_text(make_room(landlord_name="", contact_phone=""))
    assert "Chủ trọ" not in text



def test_format_room_text_keeps_all_fields_when_deposit_set():
    text = vector_store.format_room_text(make_room())
    for expected in ("Tiền cọc", "Điện", "Máy lạnh", "Không hút thuốc", "Phòng sạch sẽ", "Anh Nam", "0901234567"):
        assert expected in text


def test_format_room_text_without_optional_costs():
    text = vector_store.format_room_text(make_room(deposit=None, electricity_cost=None, water_cost=None))
    assert "Tiền cọc" not in text and "Điện" not in text
    assert "Máy lạnh" in text and "Anh Nam" in text


def test_local_embedding_is_deterministic_and_lexical():
    a = local_embedding("phòng trọ quận 7 có máy lạnh")
    assert a == local_embedding("phòng trọ quận 7 có máy lạnh")
    assert len(a) == 768
    close = sum(x * y for x, y in zip(a, local_embedding("phòng trọ quận 7 máy lạnh")))
    far = sum(x * y for x, y in zip(a, local_embedding("hợp đồng đặt cọc pháp lý")))
    assert close > far


def test_search_excludes_unavailable_and_applies_filters():
    vector_store.add_rooms([
        make_room(id="a", price=3_000_000, area_sqm=20),
        make_room(id="b", price=6_000_000, area_sqm=40, title="Căn hộ mini cao cấp"),
        make_room(id="c", available=False),
    ])
    ids = {r["id"] for r in vector_store.search_rooms("phòng trọ", n_results=10)}
    assert ids == {"a", "b"}

    cheap = vector_store.search_rooms("phòng trọ", n_results=10, max_price=4_000_000)
    assert [r["id"] for r in cheap] == ["a"]

    big = vector_store.search_rooms("phòng trọ", n_results=10, min_area=30)
    assert [r["id"] for r in big] == ["b"]


def test_search_on_empty_store_returns_empty():
    assert vector_store.search_rooms("bất kỳ") == []
    assert vector_store.search_policies("bất kỳ") == []


def test_delete_rooms():
    vector_store.add_rooms([make_room(id="x"), make_room(id="y")])
    assert vector_store.delete_rooms(["x"]) == 1
    assert [r["id"] for r in vector_store.search_rooms("phòng", n_results=10)] == ["y"]


def test_ensure_embeddings_current_restamps_stale_collections():
    vector_store.add_rooms([make_room(id="z")])
    vector_store.rooms_col.modify(metadata={"description": "old", "embedding_signature": "stale"})
    vector_store.ensure_embeddings_current()
    assert vector_store.rooms_col.metadata["embedding_signature"].startswith("local-hash")
    assert vector_store.rooms_col.count() == 1


def test_chat_endpoint_end_to_end():
    vector_store.add_rooms([make_room(id="chat1")])
    vector_store.add_policy_documents([
        {"id": "p1", "title": "Tiền cọc", "category": "hop-dong", "content": "Tiền cọc thường bằng 1 tháng tiền thuê."}
    ])
    client = TestClient(app)
    res = client.post("/api/v1/chat", json={"message": "Tìm phòng ở Hai Bà Trưng", "filters": {"district": "Hai Bà Trưng"}})
    assert res.status_code == 200
    body = res.json()
    assert body["answer"]
    assert body["matched_rooms"][0]["id"] == "chat1"
    assert body["matched_rooms"][0]["landlord_name"] == "Anh Nam"
    assert body["matched_rooms"][0]["contact_phone"] == "0901234567"
    assert "price_assessment" in body["matched_rooms"][0]
    assert body["matched_rooms"][0]["price_assessment"]["label"] in ("GIÁ TỐT", "GIÁ HỢP LÝ", "GIÁ CAO")
    assert body["source_documents"] == ["Tiền cọc"]


def test_evaluate_room_price_labels():
    from app.services.pricing_service import pricing_service

    # Cầu Giấy base rate is 160k/m2. For 25m2 + máy lạnh (400k) -> estimated ~4.400.000
    cheap = pricing_service.evaluate_room_price(
        price=3_000_000, area_sqm=25, district="Cầu Giấy", amenities=["máy lạnh"]
    )
    assert cheap["label"] == "GIÁ TỐT"
    assert cheap["diff_percent"] < -8.0

    fair = pricing_service.evaluate_room_price(
        price=4_400_000, area_sqm=25, district="Cầu Giấy", amenities=["máy lạnh"]
    )
    assert fair["label"] == "GIÁ HỢP LÝ"
    assert -8.0 <= fair["diff_percent"] <= 8.0

    expensive = pricing_service.evaluate_room_price(
        price=6_500_000, area_sqm=25, district="Cầu Giấy", amenities=["máy lạnh"]
    )
    assert expensive["label"] == "GIÁ CAO"
    assert expensive["diff_percent"] > 8.0


def test_ingest_delete_and_error_hides_internals():
    client = TestClient(app)
    room = make_room(id="api1").model_dump()
    assert client.post("/api/v1/ingest/rooms", json={"rooms": [room]}).json()["indexed_count"] == 1
    assert client.delete("/api/v1/ingest/rooms", params={"ids": ["api1"]}).json()["deleted_count"] == 1
    assert client.post("/api/v1/ingest/rooms", json={"rooms": [{"bad": 1}]}).status_code == 422
