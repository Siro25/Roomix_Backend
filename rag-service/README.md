# Roomix RAG Service 🏠🤖

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
│ - roomix_rooms: Lập chỉ mục thông tin & tiện ích phòng │
│ - roomix_policies: Bộ luật nhà ở & chính sách cọc/thuê │
└────────────────────────────────────────────────────────┘
                     │
                     ▼
┌────────────────────────────────────────────────────────┐
│             Google Gemini AI Engine                   │
│ - Gemini 1.5 / 2.0 Flash: Tạo câu trả lời & Phân tích  │
│ - text-embedding-004: Vector hóa câu hỏi & phòng trọ   │
└────────────────────────────────────────────────────────┘
```

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
copy .env.example .env
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
Khi khởi động lần đầu, hệ thống sẽ tự động nạp dữ liệu mẫu ban đầu gồm các phòng trọ thực tế tại TP.HCM (Quận 1, Quận 7, Bình Thạnh, Gò Vấp, Thủ Đức) và cẩm nang pháp lý thuê trọ vào ChromaDB.

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
