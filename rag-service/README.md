# Roomix RAG Service — LangChain

Hệ thống AI RAG (Retrieval-Augmented Generation) chuyên sâu phục vụ:
1. **Phân tích dự đoán & gợi ý định giá phòng cho chủ trọ** dựa trên vị trí, diện tích, tiện ích và thị trường cạnh tranh.
2. **Chatbot AI tư vấn tìm phòng trọ thông minh** theo tiêu chí (giá, quận/huyện, tiện ích) & **hỗ trợ giải đáp chính sách** (tiền cọc, điện nước, hợp đồng thuê phòng theo luật cư trú Việt Nam).

---

## 🌟 Kiến trúc giải pháp (Architecture)

```text
[ Người dùng / Frontend / Gateway (Port 8083) ]
                     │
                     ▼
          [ FastAPI Application ]
          /           │          \
         /            │           \
   /chat          /pricing       /ingest
(RAG Assistant) (Hedonic Engine) (Room/Policy Sync)
         │            │           │
         ▼            ▼           ▼
┌────────────────────────────────────────────────────────┐
│             ChromaDB Vector Store                      │
│ - *_rooms: Lập chỉ mục thông tin & tiện ích phòng      │
│ - *_policies: Nội dung chính sách cọc/thuê mẫu          │
└────────────────────────────────────────────────────────┘
                     │
                     ▼
┌────────────────────────────────────────────────────────┐
│           LangChain + Google Gemini                   │
│ - ChatGoogleGenerativeAI: Tạo câu trả lời, phân tích   │
│ - GoogleGenerativeAIEmbeddings: Vector hóa văn bản    │
└────────────────────────────────────────────────────────┘
```

---

## Luồng LangChain

Service dùng `langchain-core`, `langchain-google-genai` và `langchain-chroma`.
Không cần cài gói tổng `langchain` hoặc cấu hình LangSmith để chạy.

- `app/core/gemini_client.py`: khởi tạo `ChatGoogleGenerativeAI` và
  `GoogleGenerativeAIEmbeddings` từ các biến `GEMINI_*` trong `.env`.
- `app/core/vector_store.py`: chuyển phòng/quy định thành `Document`, gọi
  `Chroma.add_documents()` với ID cố định để thêm/cập nhật. Phòng được tìm bằng
  `similarity_search_with_score()` cùng metadata filter; quy định dùng
  `as_retriever().invoke()`.
- `app/services/rag_chat_service.py`: lấy tối đa 4 phòng, 2 quy định, thêm nhận xét
  CSV nếu có; đưa 6 tin nhắn gần nhất vào `MessagesPlaceholder`, rồi gọi chuỗi
  `ChatPromptTemplate | ChatGoogleGenerativeAI | StrOutputParser`.
- `pricing_service.py`: giữ công thức tính giá hiện có; phần nhận xét gọi trực tiếp
  `self.chain.invoke()` với chuỗi prompt/model/parser như chatbot.
  Giá đề xuất không phải kết quả của mô hình học máy.

API và cấu trúc request/response giữ nguyên. Bộ lọc vị trí/giá/loại phòng vẫn cần
được gửi trong `filters`; LangChain không tự trích xuất chúng từ câu hỏi.

### Chuyển từ chỉ mục cũ

Bản LangChain dùng collection mới trong `CHROMA_PERSIST_DIRECTORY`:

- `roomix_lc_v1_google_<hash-model>_rooms` và collection `_policies`.
- API key là bắt buộc; ứng dụng không còn model hoặc embedding giả lập.

Các collection cũ `roomix_rooms` / `roomix_policies` không bị xóa hay tự chuyển.
Cần tạo lại embedding vì phiên bản trước có thể chứa vector giả lập và văn bản
phòng bị thiếu trường. Khi chuyển sang API key thật hoặc đổi embedding model,
service cũng sử dụng chỉ mục riêng, tránh trộn các không gian vector.

Lần đầu chạy với chỉ mục rỗng, service tự nạp dữ liệu mẫu. Nếu có phòng thực tế,
gửi lại danh sách phòng qua `POST /api/v1/ingest/rooms`. Gọi
`POST /api/v1/ingest/init-sample-data` để nạp lại các JSON mẫu sau khi sửa chúng.
Các thao tác này upsert theo ID, không xóa bản ghi vắng mặt trong request.

Nếu thiếu API key hoặc vẫn để giá trị mẫu, service báo lỗi cấu hình khi khởi động.
Lỗi Gemini được trả về qua lỗi API, không có cơ chế chuyển sang vector giả. Lỗi truy vấn cũng không bỏ bộ lọc để tìm lại.

Model chat và embedding được lấy từ `.env`; cần chọn model được tài khoản Gemini
hỗ trợ. Việc cài LangChain không xác nhận model đang cấu hình có khả dụng.

### Kiểm thử offline

```bash
cd rag-service
python -m pip install -r requirements-dev.txt
python -m pytest tests -q
```

Tests tự thay provider bằng test double trong `tests/conftest.py`, dùng ChromaDB
thật trong thư mục tạm. Việc này chỉ áp dụng trong tests, không phải chế độ chạy
của ứng dụng. Tests không dùng
API key trong `.env`, không sửa chỉ mục đang chạy. Kiểm thử bao gồm upsert,
persistence, bộ lọc, truy xuất chính sách, lịch sử/prompt và các API chat/pricing.

Tham khảo tích hợp chính thức:
[Gemini chat](https://docs.langchain.com/oss/python/integrations/chat/google_generative_ai),
[Gemini embeddings](https://docs.langchain.com/oss/python/integrations/embeddings/google_generative_ai),
[Chroma](https://docs.langchain.com/oss/python/integrations/vectorstores/chroma).

---

## 🚀 Hướng dẫn cài đặt & Chạy ứng dụng

### 1. Tạo môi trường ảo Python
```bash
cd rag-service
python -m venv .venv

# Trên Windows PowerShell:
.\.venv\Scripts\Activate.ps1

# Trên Linux/macOS:
source .venv/bin/activate
```

### 2. Cài đặt các thư viện cần thiết
```bash
pip install -r requirements.txt
```

### 3. Cấu hình biến môi trường
Tạo file `.env` từ file `.env.example`:
```bash
cp .env.example .env  # Chỉ chạy khi chưa có .env
```
Mở file `.env` và điền khóa API của bạn:
```env
GEMINI_API_KEY=AIzaSy...
GEMINI_MODEL=gemini-1.5-flash
GEMINI_EMBEDDING_MODEL=models/text-embedding-004
SERVICE_PORT=8083
```

### 4. Khởi động dịch vụ
```bash
python -m uvicorn app.main:app --host 0.0.0.0 --port 8083 --reload
```
Khi khởi động lần đầu, hệ thống sẽ tự động nạp dữ liệu mẫu ban đầu gồm 5 phòng mẫu tại TP.HCM và 4 tài liệu quy định thuê trọ mẫu vào ChromaDB.

Truy cập Swagger UI tài liệu API tương tác tại:
👉 `http://localhost:8083/docs`

---

## 📡 Chi tiết API Endpoints

### 1. Trợ lý Chat RAG (`POST /api/v1/chat`)
Tư vấn tìm phòng theo tiêu chí tự nhiên hoặc hỏi đáp chính sách cọc, hợp đồng.

**Body Request:**
```json
{
  "message": "Tôi là sinh viên cần tìm phòng trọ tại Quận 7 hoặc gần đó, giá dưới 4.5 triệu có máy lạnh và gác lửng",
  "user_role": "TENANT",
  "filters": {
    "max_price": 5000000,
    "district": "Quận 7"
  }
}
```

**Response mẫu:**
```json
{
  "answer": "Chào bạn! Roomix có phòng rất phù hợp với tiêu chí của bạn tại Quận 7: **Phòng trọ Full nội thất có gác lửng gần ĐH Tôn Đức Thắng**...",
  "matched_rooms": [
    {
      "id": "room-001",
      "title": "Phòng trọ Full nội thất có gác lửng cao cấp gần ĐH Tôn Đức Thắng",
      "district": "Quận 7",
      "price": 4200000.0,
      "area_sqm": 25.0,
      "amenities": ["Máy lạnh", "Tủ lạnh", "Gác lửng", "Nóng lạnh", "Wifi"],
      "contact_phone": "0901234567",
      "landlord_name": "Chú Hùng"
    }
  ],
  "suggested_follow_ups": [
    "Chi phí điện nước và phụ phí của các phòng trên thế nào?",
    "Làm sao để đặt lịch hẹn xem phòng với chủ trọ?"
  ]
}
```

---

### 2. Dự đoán định giá phòng cho Chủ trọ (`POST /api/v1/pricing/predict`)
Tính toán mức giá tối ưu và phân tích insight thị trường cạnh tranh.

**Body Request:**
```json
{
  "city": "TP Hồ Chí Minh",
  "district": "Quận 7",
  "ward": "Tân Phong",
  "room_type": "PHONG_TRO",
  "area_sqm": 24.0,
  "amenities": ["Máy lạnh", "Tủ lạnh", "Bình nóng lạnh", "Máy giặt"],
  "is_mezzanine": true,
  "is_studio": false,
  "has_parking": true,
  "is_newly_built": true
}
```

**Response mẫu:**
```json
{
  "recommended_price": 4350000.0,
  "estimated_price_min": 3900000.0,
  "estimated_price_max": 5000000.0,
  "confidence_score": 0.88,
  "breakdown": {
    "base_price_per_sqm": 150000.0,
    "district_weight": 1.0,
    "area_cost": 3600000.0,
    "amenities_surcharge": 1050000.0,
    "feature_bonus": 882000.0
  },
  "market_analysis": "### Đánh giá cạnh tranh...\n- **Khách hàng mục tiêu**: Sinh viên ĐH Tôn Đức Thắng, RMIT hoặc nhân viên văn phòng khu Phú Mỹ Hưng.\n- **Dự báo tỷ lệ lấp đầy**: Đạt 95% trong vòng 1-2 tuần kể từ khi đăng bài...",
  "comparable_rooms": [...]
}
```

---

### 3. Đồng bộ phòng mới vào Vector DB (`POST /api/v1/ingest/rooms`)
Dành cho `rental-service` hoặc admin khi có phòng trọ mới được duyệt đăng.

---

## Cấu trúc và file chuyển từ prototype Aichatbot

```text
rag-service/
├── app/
│   ├── api/                       # FastAPI endpoints
│   ├── core/                      # Gemini client và ChromaDB
│   └── services/                  # Nghiệp vụ chat, định giá, đánh giá thị trường
├── data/market/                   # CSV thị trường đầu vào của MarketEvaluator
└── scripts/
    └── crawlers/                  # Công cụ thu thập dữ liệu chạy độc lập
```

ChromaDB là vector store duy nhất của service. Thông tin phòng và chính sách được lưu trong các collection riêng; `MarketEvaluator` đọc CSV để phân tích giá, không dùng thêm vector store.

| File nguồn | Vị trí/trách nhiệm trong service |
| --- | --- |
| `chatbot.py` | Luồng chatbot API đã có tại `app/api/routes_chat.py` và `app/services/rag_chat_service.py`. Script CLI cũ dựng index và chờ `input()` ngay khi import nên không được chép nguyên trạng vào app. |
| `market_evaluator.py` | `app/services/market_evaluator.py`; `market_data_service.py` dùng nó cho phần so sánh giá ở chatbot và API định giá nếu cấu hình `MARKET_DATA_CSV`. |
| `tool/tool.py` | `scripts/crawlers/phongtro123_hanoi.py`; chạy thủ công, không chạy cùng FastAPI. |

Crawler có thể tạo CSV theo đúng schema của bộ đánh giá:

```powershell
cd rag-service
pip install -r scripts/crawlers/requirements.txt
python scripts/crawlers/phongtro123_hanoi.py --pages 5 --output data/market/phongtro_hanoi.csv
```

`MARKET_DATA_CSV` mặc định là `data/market/phongtro_hanoi.csv`. Có thể đổi biến này trong `.env`. CSV hiện chưa được chép từ máy nguồn; khi chưa có CSV, API vẫn dùng luồng định giá và các phòng so sánh từ ChromaDB.
