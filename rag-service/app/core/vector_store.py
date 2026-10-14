import json
import logging
from typing import List, Dict, Any, Optional

import chromadb
from chromadb.config import Settings as ChromaSettings
from langchain_chroma import Chroma
from langchain_core.documents import Document

from app.config import settings
from app.core.gemini_client import gemini_client
from app.models.schemas import RoomItem

logger = logging.getLogger(__name__)


# quản lý vector store cho phòng trọ và quy định thuê
# xử lí hai việc chính đó là nạp dữ liệu : Phòng//quy định → Document → Embedding → ChromaDB
# Tìm dữ liệu: Câu hỏi → Embedding → Tìm trong ChromaDB → Kết quả
class VectorStoreManager:
    def __init__(self):
        # Khởi tạo ChromaDB client với thư mục lưu trữ được cấu hình trong .env
        self.client = chromadb.PersistentClient(
            path=settings.CHROMA_PERSIST_DIRECTORY,
            settings=ChromaSettings(anonymized_telemetry=False),
        )


        namespace = gemini_client.index_namespace
        # vector db cho phòng trọ
        self.rooms_store = Chroma(
            client=self.client,
            collection_name=f"{namespace}_rooms",
            embedding_function=gemini_client.embeddings,
        )
        # vector db cho các quy định thuê
        self.policies_store = Chroma(
            client=self.client,
            collection_name=f"{namespace}_policies",
            embedding_function=gemini_client.embeddings,
        )
        # hai collection dùng chung dùng để đếm bản ghi và kiểm tra xem có dữ liệu hay không
        self.rooms_col = self.client.get_collection(f"{namespace}_rooms")
        self.policies_col = self.client.get_collection(f"{namespace}_policies")

        logger.info("LangChain Chroma initialized: %s", namespace)

    # Giữ giao diện đếm bản ghi mà startup và health check sử dụng.
    def count_rooms(self) -> int:
        return self.rooms_col.count()

    def count_policies(self) -> int:
        return self.policies_col.count()

    # format thông tin phòng trọ thành một chuỗi văn bản để lưu vào vector store
    def format_room_text(self, room: RoomItem) -> str:
        amenities = ", ".join(room.amenities) if room.amenities else "Cơ bản"
        rules = ", ".join(room.rules) if room.rules else "Không có nội quy đặc biệt"
        lines = [
            f"Phòng: {room.title}",
            f"Loại hình: {room.room_type}",
            f"Địa chỉ: {room.address}, {room.ward or ''}, {room.district}, {room.city}",
            f"Diện tích: {room.area_sqm} m2",
            f"Giá thuê: {room.price:,.0f} VND/tháng",
        ]
        if room.deposit is not None:
            lines.append(f"Tiền cọc: {room.deposit:,.0f} VND")
        if room.electricity_cost is not None:
            lines.append(f"Điện: {room.electricity_cost:,.0f} VND/số")
        if room.water_cost is not None:
            lines.append(f"Nước: {room.water_cost:,.0f} VND")
        lines.extend([
            f"Tiện nghi: {amenities}",
            f"Nội quy: {rules}",
            f"Mô tả: {room.description or ''}",
            f"Chủ trọ: {room.landlord_name or ''} - SĐT: {room.contact_phone or ''}",
        ])
        return "\n".join(lines)

    # Thêm danh sách phòng trọ vào chorma, trả về số lượng bản ghi đã thêm
    def add_rooms(self, rooms: List[RoomItem]) -> int:
        if not rooms:
            return 0
        documents = [
            Document(
                id=str(room.id),
                page_content=self.format_room_text(room),
                metadata={
                    "id": str(room.id),
                    "title": room.title,
                    "city": room.city,
                    "district": room.district,
                    "ward": room.ward or "",
                    "room_type": room.room_type,
                    "price": float(room.price),
                    "area_sqm": float(room.area_sqm),
                    "contact_phone": room.contact_phone or "",
                    "landlord_name": room.landlord_name or "",
                    "amenities": json.dumps(room.amenities, ensure_ascii=False),
                    "available": "true" if room.available else "false",
                },
            )
            for room in rooms
        ]
        # thêm document của room vừa tạo và chorma, sẽ embedding và lưu trữ vào collection rooms_store
        self.rooms_store.add_documents(
            documents, 
            ids=[str(room.id) for room in rooms]
        )

        logger.info("Indexed %s rooms into vector store.", len(rooms))
        return len(rooms)
    
    # Tìm kiếm phòng trọ trong chorma
    def search_rooms(
        self,
        query: str,
        n_results: int = 5,
        city: Optional[str] = None,
        district: Optional[str] = None,
        min_price: Optional[float] = None,
        max_price: Optional[float] = None,
        room_type: Optional[str] = None,
    ) -> List[Dict[str, Any]]:

        if self.count_rooms() == 0:
            return []

        # chứa các điều kiện cần áp dụng 
        clauses = []
        
        for field, value in (("city", city), ("district", district), ("room_type", room_type)):
            if value:
                clauses.append({field: {"$eq": value}})
        if min_price is not None:
            clauses.append({"price": {"$gte": float(min_price)}})
        if max_price is not None:
            clauses.append({"price": {"$lte": float(max_price)}})

        if len(clauses) == 1:
            where = clauses[0]
        elif clauses:
            where = {"$and": clauses}
        else:
            where = None

        results = self.rooms_store.similarity_search_with_score(
            query, 
            k=n_results, 
            filter=where,# điều kiện áp dụng để lọc các phòng trọ theo bộ lọc của người dùng
        )
        return [
            {
                "id": doc.id,
                "document": doc.page_content,
                "metadata": doc.metadata,
                "distance": distance,
            }
            for doc, distance in results
        ]

    # Thêm danh sách quy định thuê vào chorma, trả về số lượng bản ghi đã thêm
    def add_policy_documents(self, docs: List[Dict[str, Any]]) -> int:
        if not docs:
            return 0
        documents = [
            Document(
                id=doc["id"],
                page_content=(
                    f"Tiêu đề: {doc.get('title', '')}\n"
                    f"Chủ đề: {doc.get('category', '')}\n"
                    f"Nội dung: {doc.get('content', '')}"
                ),
                metadata={
                    "title": doc.get("title", ""),
                    "category": doc.get("category", ""),
                },
            )
            for doc in docs
        ]
        self.policies_store.add_documents(
            documents,
            ids=[doc["id"] for doc in docs]
        )
        return len(docs)

    # tìm kiếm các quy định thuê trong chorma
    def search_policies(self, query: str, n_results: int = 3) -> List[Dict[str, Any]]:
        if self.count_policies() == 0:
            return []
        retriever = self.policies_store.as_retriever(search_kwargs={"k": n_results})
        return [
            {"id": doc.id, "document": doc.page_content, "metadata": doc.metadata}
            for doc in retriever.invoke(query)
        ]


vector_store = VectorStoreManager()
