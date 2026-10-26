import os
import json
import logging
from typing import List, Dict, Any, Optional
import chromadb
from chromadb.config import Settings as ChromaSettings
from app.config import settings
from app.core.gemini_client import gemini_client
from app.models.schemas import RoomItem

logger = logging.getLogger(__name__)

ROOMS_DESCRIPTION = "Vector store for rental room listings"
POLICIES_DESCRIPTION = "Vector store for tenancy policies, contracts, and regulations"
SIGNATURE_KEY = "embedding_signature"


class VectorStoreManager:
    def __init__(self):
        os.makedirs(settings.CHROMA_PERSIST_DIRECTORY, exist_ok=True)
        self.client = chromadb.PersistentClient(
            path=settings.CHROMA_PERSIST_DIRECTORY,
            settings=ChromaSettings(anonymized_telemetry=False)
        )
        self.rooms_col = self.client.get_or_create_collection(
            name="roomix_rooms",
            metadata={"description": ROOMS_DESCRIPTION}
        )
        self.policies_col = self.client.get_or_create_collection(
            name="roomix_policies",
            metadata={"description": POLICIES_DESCRIPTION}
        )
        logger.info("ChromaDB vector store initialized successfully.")

    # ------------------------------------------------------------------
    # Embedding-space consistency
    # ------------------------------------------------------------------
    def ensure_embeddings_current(self) -> None:
        """Re-embed stored documents if they were indexed with another embedding space.

        Vectors from a different model/dimension (or from the offline fallback)
        cannot be compared with query vectors, so retrieval would be garbage.
        """
        signature = gemini_client.embedding_signature
        for col, description in (
            (self.rooms_col, ROOMS_DESCRIPTION),
            (self.policies_col, POLICIES_DESCRIPTION),
        ):
            current = (col.metadata or {}).get(SIGNATURE_KEY)
            if current == signature:
                continue
            count = col.count()
            if count:
                logger.info(
                    f"Re-embedding {count} documents in '{col.name}' ({current} -> {signature})."
                )
                data = col.get(include=["documents", "metadatas"])
                embeddings = gemini_client.get_embeddings(
                    data["documents"], task_type="retrieval_document"
                )
                col.upsert(
                    ids=data["ids"],
                    embeddings=embeddings,
                    documents=data["documents"],
                    metadatas=data["metadatas"],
                )
            col.modify(metadata={"description": description, SIGNATURE_KEY: signature})

    def _stamp_signature(self, col, description: str) -> None:
        signature = gemini_client.embedding_signature
        if (col.metadata or {}).get(SIGNATURE_KEY) != signature:
            col.modify(metadata={"description": description, SIGNATURE_KEY: signature})

    # ------------------------------------------------------------------
    # Rooms
    # ------------------------------------------------------------------
    def format_room_text(self, room: RoomItem) -> str:
        amenities_str = ", ".join(room.amenities) if room.amenities else "Cơ bản"
        rules_str = ", ".join(room.rules) if room.rules else "Không có nội quy đặc biệt"

        lines = [
            f"Phòng: {room.title}",
            f"Loại hình: {room.room_type}",
            f"Địa chỉ: {', '.join(p for p in (room.address, room.ward, room.district, room.city) if p)}",
            f"Diện tích: {room.area_sqm} m2",
            f"Giá thuê: {room.price:,.0f} VND/tháng",
        ]
        if room.deposit:
            lines.append(f"Tiền cọc: {room.deposit:,.0f} VND")
        if room.electricity_cost or room.water_cost:
            utilities = []
            if room.electricity_cost:
                utilities.append(f"Điện: {room.electricity_cost:,.0f} VND/số")
            if room.water_cost:
                utilities.append(f"Nước: {room.water_cost:,.0f} VND")
            lines.append(", ".join(utilities))
        extra = []
        if room.house_name or room.room_number:
            extra.append(f"Nhà/Phòng: {' - '.join(p for p in (room.house_name, room.room_number) if p)}")
        if room.max_tenants:
            extra.append(f"Số người tối đa: {room.max_tenants}")
        if room.available_from:
            extra.append(f"Có thể dọn vào từ: {room.available_from}")
        lines += extra
        lines += [
            f"Tiện nghi: {amenities_str}",
            f"Nội quy: {rules_str}",
            f"Mô tả: {room.description or 'Chưa có mô tả'}",
        ]
        if room.landlord_name or room.contact_phone:
            lines.append(f"Chủ trọ: {room.landlord_name or 'Chưa rõ'} - SĐT: {room.contact_phone or 'Chưa có'}")
        return "\n".join(lines)

    @staticmethod
    def _room_metadata(r: RoomItem) -> Dict[str, Any]:
        return {
            "id": str(r.id),
            "title": r.title,
            "city": r.city,
            "district": r.district,
            "ward": r.ward or "",
            "address": r.address or "",
            "room_type": r.room_type,
            "price": float(r.price),
            "area_sqm": float(r.area_sqm),
            "deposit": float(r.deposit) if r.deposit else 0.0,
            "max_tenants": int(r.max_tenants) if r.max_tenants else 0,
            "image_url": r.image_url or "",
            "contact_phone": r.contact_phone or "",
            "landlord_name": r.landlord_name or "",
            "amenities": json.dumps(r.amenities or [], ensure_ascii=False),
            "available": "true" if r.available else "false"
        }

    def add_rooms(self, rooms: List[RoomItem]):
        if not rooms:
            return 0
        documents = [self.format_room_text(r) for r in rooms]
        embeddings = gemini_client.get_embeddings(documents, task_type="retrieval_document")
        metadatas = [self._room_metadata(r) for r in rooms]
        ids = [str(r.id) for r in rooms]

        self.rooms_col.upsert(
            ids=ids,
            embeddings=embeddings,
            documents=documents,
            metadatas=metadatas
        )
        self._stamp_signature(self.rooms_col, ROOMS_DESCRIPTION)
        logger.info(f"Indexed {len(rooms)} rooms into vector store.")
        return len(rooms)

    def sync_rooms(self, rooms: List[RoomItem]) -> Dict[str, int]:
        """Make the room index mirror `rooms` exactly.

        Only new or changed rooms are (re-)embedded; rooms that are no longer
        in `rooms` (rented, unpublished, deleted) are removed.
        """
        incoming = {str(r.id): r for r in rooms}
        existing = self.rooms_col.get(include=["documents", "metadatas"])
        existing_by_id = {
            i: (doc, meta)
            for i, doc, meta in zip(existing["ids"], existing["documents"], existing["metadatas"])
        }

        to_index: List[RoomItem] = []
        for room_id, room in incoming.items():
            previous = existing_by_id.get(room_id)
            if previous is None:
                to_index.append(room)
            elif previous[0] != self.format_room_text(room) or previous[1] != self._room_metadata(room):
                to_index.append(room)

        stale_ids = [i for i in existing_by_id if i not in incoming]
        if stale_ids:
            self.rooms_col.delete(ids=stale_ids)
        if to_index:
            self.add_rooms(to_index)
        added = sum(1 for r in to_index if str(r.id) not in existing_by_id)
        return {
            "total": len(incoming),
            "added": added,
            "updated": len(to_index) - added,
            "removed": len(stale_ids),
            "unchanged": len(incoming) - len(to_index),
        }

    def known_locations(self) -> Dict[str, List[str]]:
        """Distinct cities/districts/wards currently indexed (for query parsing)."""
        data = self.rooms_col.get(include=["metadatas"])
        result: Dict[str, set] = {"city": set(), "district": set(), "ward": set()}
        for meta in data["metadatas"]:
            for key in result:
                if meta.get(key):
                    result[key].add(meta[key])
        return {k: sorted(v) for k, v in result.items()}

    def delete_rooms(self, room_ids: List[str]) -> int:
        ids = [str(i) for i in room_ids]
        if not ids:
            return 0
        self.rooms_col.delete(ids=ids)
        return len(ids)

    @staticmethod
    def build_room_filter(
        city: Optional[str] = None,
        district: Optional[str] = None,
        min_price: Optional[float] = None,
        max_price: Optional[float] = None,
        room_type: Optional[str] = None,
        min_area: Optional[float] = None,
        max_area: Optional[float] = None,
        only_available: bool = True,
        ward: Optional[str] = None,
    ) -> Optional[Dict[str, Any]]:
        where_clauses = []
        if only_available:
            where_clauses.append({"available": {"$ne": "false"}})
        if city:
            where_clauses.append({"city": {"$eq": city}})
        if district:
            where_clauses.append({"district": {"$eq": district}})
        if ward:
            where_clauses.append({"ward": {"$eq": ward}})
        if room_type:
            where_clauses.append({"room_type": {"$eq": room_type}})
        if min_price is not None:
            where_clauses.append({"price": {"$gte": float(min_price)}})
        if max_price is not None:
            where_clauses.append({"price": {"$lte": float(max_price)}})
        if min_area is not None:
            where_clauses.append({"area_sqm": {"$gte": float(min_area)}})
        if max_area is not None:
            where_clauses.append({"area_sqm": {"$lte": float(max_area)}})

        if not where_clauses:
            return None
        if len(where_clauses) == 1:
            return where_clauses[0]
        return {"$and": where_clauses}

    def search_rooms(
        self,
        query: str,
        n_results: int = 5,
        city: Optional[str] = None,
        district: Optional[str] = None,
        min_price: Optional[float] = None,
        max_price: Optional[float] = None,
        room_type: Optional[str] = None,
        min_area: Optional[float] = None,
        max_area: Optional[float] = None,
        only_available: bool = True,
        ward: Optional[str] = None,
    ) -> List[Dict[str, Any]]:
        total = self.rooms_col.count()
        if total == 0:
            return []

        query_emb = gemini_client.get_embedding(query, task_type="retrieval_query")
        where = self.build_room_filter(
            city=city, district=district, min_price=min_price, max_price=max_price,
            room_type=room_type, min_area=min_area, max_area=max_area,
            only_available=only_available, ward=ward,
        )

        try:
            results = self.rooms_col.query(
                query_embeddings=[query_emb],
                n_results=min(n_results, total),
                where=where
            )
        except Exception as e:
            logger.warning(f"Chroma rooms query failed ({e}), attempting fallback.")
            try:
                results = self.rooms_col.query(
                    query_embeddings=[query_emb],
                    n_results=min(n_results, total),
                )
            except Exception as e2:
                logger.error(f"Fallback Chroma query also failed: {e2}")
                return []
        return self._format_results(results, include_distance=True)

    # ------------------------------------------------------------------
    # Policies
    # ------------------------------------------------------------------
    def add_policy_documents(self, docs: List[Dict[str, Any]]):
        if not docs:
            return 0
        ids = [d.get("id") for d in docs]
        documents = [
            f"Tiêu đề: {d.get('title')}\nChủ đề: {d.get('category')}\nNội dung: {d.get('content')}"
            for d in docs
        ]
        embeddings = gemini_client.get_embeddings(documents, task_type="retrieval_document")
        metadatas = [
            {"title": d.get("title", ""), "category": d.get("category", "")}
            for d in docs
        ]

        self.policies_col.upsert(
            ids=ids,
            embeddings=embeddings,
            documents=documents,
            metadatas=metadatas
        )
        self._stamp_signature(self.policies_col, POLICIES_DESCRIPTION)
        return len(docs)

    def search_policies(self, query: str, n_results: int = 3) -> List[Dict[str, Any]]:
        total = self.policies_col.count()
        if total == 0:
            return []
        query_emb = gemini_client.get_embedding(query, task_type="retrieval_query")
        try:
            results = self.policies_col.query(
                query_embeddings=[query_emb],
                n_results=min(n_results, total)
            )
        except Exception as e:
            logger.error(f"Chroma policies query failed: {e}")
            return []
        return self._format_results(results, include_distance=False)

    @staticmethod
    def _format_results(results: Dict[str, Any], include_distance: bool) -> List[Dict[str, Any]]:
        matched = []
        if results and results.get("ids") and len(results["ids"]) > 0:
            for i in range(len(results["ids"][0])):
                item = {
                    "id": results["ids"][0][i],
                    "document": results["documents"][0][i],
                    "metadata": results["metadatas"][0][i],
                }
                if include_distance:
                    distances = results.get("distances")
                    item["distance"] = distances[0][i] if distances else None
                matched.append(item)
        return matched


vector_store = VectorStoreManager()
