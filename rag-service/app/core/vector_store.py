import json
import logging
import os
from typing import Any, Optional

from langchain_chroma import Chroma
from langchain_core.documents import Document

from app.config import settings
from app.core.langchain_components import embeddings
from app.models.schemas import RoomItem

logger = logging.getLogger(__name__)


class VectorStoreManager:
    """Room and policy retrieval backed by LangChain's Chroma integration."""

    def __init__(self):
        os.makedirs(settings.CHROMA_PERSIST_DIRECTORY, exist_ok=True)
        self.rooms = Chroma(
            collection_name="roomix_rooms",
            embedding_function=embeddings,
            persist_directory=settings.CHROMA_PERSIST_DIRECTORY,
        )
        self.policies = Chroma(
            collection_name="roomix_policies",
            embedding_function=embeddings,
            persist_directory=settings.CHROMA_PERSIST_DIRECTORY,
        )
        logger.info("LangChain Chroma vector stores initialized successfully.")

    def count_rooms(self) -> int:
        return len(self.rooms.get(include=[]).get("ids", []))

    def count_policies(self) -> int:
        return len(self.policies.get(include=[]).get("ids", []))

    def format_room_text(self, room: RoomItem) -> str:
        amenities = ", ".join(room.amenities) if room.amenities else "Cơ bản"
        rules = ", ".join(room.rules) if room.rules else "Không có nội quy đặc biệt"
        lines = [
            f"Phòng: {room.title}",
            f"Loại hình: {room.room_type}",
            f"Địa chỉ: {room.address}, {room.ward}, {room.district}, {room.city}",
            f"Diện tích: {room.area_sqm} m2",
            f"Giá thuê: {room.price:,.0f} VND/tháng",
        ]
        if room.deposit is not None:
            lines.append(f"Tiền cọc: {room.deposit:,.0f} VND")
        if room.electricity_cost is not None:
            lines.append(f"Tiền điện: {room.electricity_cost:,.0f} VND/số")
        if room.water_cost is not None:
            lines.append(f"Tiền nước: {room.water_cost:,.0f} VND")
        lines.extend([
            f"Tiện nghi: {amenities}",
            f"Nội quy: {rules}",
            f"Mô tả: {room.description or ''}",
            f"Chủ trọ: {room.landlord_name or ''} - SĐT: {room.contact_phone or ''}",
        ])
        return "\n".join(lines)

    def add_rooms(self, rooms: list[RoomItem]) -> int:
        if not rooms:
            return 0

        documents = []
        ids = []
        for room in rooms:
            room_id = str(room.id)
            documents.append(Document(
                id=room_id,
                page_content=self.format_room_text(room),
                metadata={
                    "id": room_id,
                    "title": room.title,
                    "city": room.city,
                    "district": room.district,
                    "ward": room.ward or "",
                    "room_type": room.room_type,
                    "price": float(room.price),
                    "area_sqm": float(room.area_sqm),
                    "contact_phone": room.contact_phone or "",
                    "landlord_name": room.landlord_name or "",
                    "amenities": json.dumps(room.amenities or [], ensure_ascii=False),
                    "available": "true" if room.available else "false",
                },
            ))
            ids.append(room_id)

        self.rooms.add_documents(documents=documents, ids=ids)
        logger.info("Indexed %s rooms through LangChain into Chroma.", len(rooms))
        return len(rooms)

    def search_rooms(
        self,
        query: str,
        n_results: int = 5,
        city: Optional[str] = None,
        district: Optional[str] = None,
        min_price: Optional[float] = None,
        max_price: Optional[float] = None,
        room_type: Optional[str] = None,
    ) -> list[dict[str, Any]]:
        filters: list[dict[str, Any]] = []
        if city:
            filters.append({"city": {"$eq": city}})
        if district:
            filters.append({"district": {"$eq": district}})
        if room_type:
            filters.append({"room_type": {"$eq": room_type}})
        if min_price is not None:
            filters.append({"price": {"$gte": float(min_price)}})
        if max_price is not None:
            filters.append({"price": {"$lte": float(max_price)}})

        metadata_filter = None
        if len(filters) == 1:
            metadata_filter = filters[0]
        elif filters:
            metadata_filter = {"$and": filters}

        results = self.rooms.similarity_search_with_score(
            query,
            k=n_results,
            filter=metadata_filter,
        )
        return [
            {
                "id": document.metadata.get("id") or document.id,
                "document": document.page_content,
                "metadata": document.metadata,
                "distance": score,
            }
            for document, score in results
        ]

    def add_policy_documents(self, docs: list[dict[str, Any]]) -> int:
        if not docs:
            return 0

        documents = []
        ids = []
        for doc in docs:
            doc_id = str(doc.get("id", ""))
            documents.append(Document(
                id=doc_id,
                page_content=(
                    f"Tiêu đề: {doc.get('title', '')}\n"
                    f"Chủ đề: {doc.get('category', '')}\n"
                    f"Nội dung: {doc.get('content', '')}"
                ),
                metadata={
                    "id": doc_id,
                    "title": doc.get("title", ""),
                    "category": doc.get("category", ""),
                },
            ))
            ids.append(doc_id)

        self.policies.add_documents(documents=documents, ids=ids)
        return len(documents)

    def search_policies(self, query: str, n_results: int = 3) -> list[dict[str, Any]]:
        results = self.policies.similarity_search_with_score(query, k=n_results)
        return [
            {
                "id": document.metadata.get("id") or document.id,
                "document": document.page_content,
                "metadata": document.metadata,
                "distance": score,
            }
            for document, score in results
        ]


vector_store = VectorStoreManager()
