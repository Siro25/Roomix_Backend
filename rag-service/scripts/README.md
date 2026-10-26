# Scripts

Các script phục vụ thu thập dữ liệu chạy độc lập với FastAPI. Chúng không được import khi service khởi động.

- `crawlers/phongtro123_hanoi.py`: thu thập tin phòng trọ Hà Nội và xuất CSV/JSON/XLSX.
- `crawlers/requirements.txt`: thư viện bổ sung chỉ cần cài khi chạy crawler.

Chạy từ thư mục `rag-service`:

```powershell
pip install -r scripts/crawlers/requirements.txt
python scripts/crawlers/phongtro123_hanoi.py --pages 5 --output data/market/phongtro_hanoi.csv
```

Crawler tự tạo thư mục đầu ra nếu chưa có. CSV được tạo theo các cột mà `app/services/market_evaluator.py` đọc.
