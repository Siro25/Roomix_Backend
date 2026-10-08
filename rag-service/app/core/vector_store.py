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

class VectorStoreManager:
    def __init__(self):
        os.makedirs(settings.CHROMA_PERSIST_DIRECTORY, exist_ok=True)
        self.client = chromadb.PersistentClient(
            path=settings.CHROMA_PERSIST_DIRECTORY,
            settings=ChromaSettings(anonymized_telemetry=False)
        )
        self.rooms_col = self.client.get_or_create_collection(
            name="roomix_rooms",
            metadata={"description": "Vector store for rental room listings"}
        )
        self.policies_col = self.client.get_or_create_collection(
            name="roomix_policies",
            metadata={"description": "Vector store for tenancy policies, contracts, and regulations"}
        )
        logger.info("ChromaDB vector store initialized successfully.")

    def format_room_text(self, room: RoomItem) -> str:
        amenities_str = ", ".join(room.amenities) if room.amenities else "Cơ bản"
        rules_str = ", ".join(room.rules) if room.rules else "Không có nội quy đặc biệt"
        text = (
            f"Phòng: {room.title}\n"
            f"Loại hình: {room.room_type}\n"
            f"Địa chỉ: {room.address}, {room.ward}, {room.district}, {room.city}\n"
            f"Diện tích: {room.area_sqm} m2\n"
            f"Giá thuê: {room.price:,.0f} VND/tháng\n"
            f"Tiền cọc: {room.deposit:,.0f} VND\n" if room.deposit else ""
            f"Điện: {room.electricity_cost:,.0f} VND/số, Nước: {room.water_cost:,.0f} VND\n" if room.electricity_cost else ""
            f"Tiện nghi: {amenities_str}\n"
            f"Nội quy: {rules_str}\n"
            f"Mô tả: {room.description}\n"
            f"Chủ trọ: {room.landlord_name} - SĐT: {room.contact_phone}"
        )
        return text

    def add_rooms(self, rooms: List[RoomItem]):
        if not rooms:
            return 0
        documents = []
        embeddings = []
        metadatas = []
        ids = []

        for r in rooms:
            doc_text = self.format_room_text(r)
            emb = gemini_client.get_embedding(doc_text, task_type="retrieval_document")
            meta = {
                "id": str(r.id),
                "title": r.title,
                "city": r.city,
                "district": r.district,
                "ward": r.ward or "",
                "room_type": r.room_type,
                "price": float(r.price),
                "area_sqm": float(r.area_sqm),
                "contact_phone": r.contact_phone or "",
                "landlord_name": r.landlord_name or "",
                "amenities": json.dumps(r.amenities or []),
                "available": "true" if r.available else "false"
            }
            documents.append(doc_text)
            embeddings.append(emb)
            metadatas.append(meta)
            ids.append(str(r.id))

        self.rooms_col.upsert(
            ids=ids,
            embeddings=embeddings,
            documents=documents,
            metadatas=metadatas
        )
        logger.info(f"Indexed {len(rooms)} rooms into vector store.")
        return len(rooms)

    def search_rooms(
        self,
        query: str,
        n_results: int = 5,
        city: Optional[str] = None,
        district: Optional[str] = None,
        min_price: Optional[float] = None,
        max_price: Optional[float] = None,
        room_type: Optional[str] = None
    ) -> List[Dict[str, Any]]:
        query_emb = gemini_client.get_embedding(query, task_type="retrieval_query")

        # Build Chroma where filters
        where_clauses = []
        if city:
            where_clauses.append({"city": {"$eq": city}})
        if district:
            where_clauses.append({"district": {"$eq": district}})
        if room_type:
            where_clauses.append({"room_type": {"$eq": room_type}})
        if min_price is not None:
            where_clauses.append({"price": {"$gte": float(min_price)}})
        if max_price is not None:
            where_clauses.append({"price": {"$lte": float(max_price)}})

        where = None
        if len(where_clauses) == 1:
            where = where_clauses[0]
        elif len(where_clauses) > 1:
            where = {"$and": where_clauses}

        try:
            results = self.rooms_col.query(
                query_embeddings=[query_emb],
                n_results=n_results,
                where=where
            )
        except Exception as e:
            logger.warning(f"Filtered query failed ({e}), falling back to unfiltered query.")
            results = self.rooms_col.query(
                query_embeddings=[query_emb],
                n_results=n_results
            )

        matched = []
        if results and results.get("ids") and len(results["ids"]) > 0:
            count = len(results["ids"][0])
            for i in range(count):
                matched.append({
                    "id": results["ids"][0][i],
                    "document": results["documents"][0][i],
                    "metadata": results["metadatas"][0][i],
                    "distance": results["distances"][0][i] if "distances" in results and results["distances"] else None
                })
        return matched

    def add_policy_documents(self, docs: List[Dict[str, Any]]):
        if not docs:
            return 0
        ids = []
        documents = []
        embeddings = []
        metadatas = []

        for d in docs:
            text = f"Tiêu đề: {d.get('title')}\nChủ đề: {d.get('category')}\nNội dung: {d.get('content')}"
            emb = gemini_client.get_embedding(text, task_type="retrieval_document")
            ids.append(d.get("id"))
            documents.append(text)
            embeddings.append(emb)
            metadatas.append({
                "title": d.get("title", ""),
                "category": d.get("category", "")
            })

        self.policies_col.upsert(
            ids=ids,
            embeddings=embeddings,
            documents=documents,
            metadatas=metadatas
        )
        return len(docs)

    def search_policies(self, query: str, n_results: int = 3) -> List[Dict[str, Any]]:
        query_emb = gemini_client.get_embedding(query, task_type="retrieval_query")
        results = self.policies_col.query(
            query_embeddings=[query_emb],
            n_results=n_results
        )
        matched = []
        if results and results.get("ids") and len(results["ids"]) > 0:
            count = len(results["ids"][0])
            for i in range(count):
                matched.append({
                    "id": results["ids"][0][i],
                    "document": results["documents"][0][i],
                    "metadata": results["metadatas"][0][i]
                })
        return matched


vector_store = VectorStoreManager()
