FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    TORCH_HOME=/app/models/torch \
    YOLO_CONFIG_DIR=/app/.tmp/ultralytics

RUN apt-get update \
    && apt-get install -y --no-install-recommends libgl1 libglib2.0-0 libgomp1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY requirements.txt constraints-runtime.txt ./
RUN python -m pip install torch==2.2.2 torchvision==0.17.2 \
        --index-url https://download.pytorch.org/whl/cpu --no-deps \
    && python -m pip install -r requirements.txt \
    && python -m pip check

RUN useradd --create-home --uid 10001 face \
    && mkdir -p /app/models /app/.tmp/ultralytics \
    && chown -R face:face /app
COPY --chown=face:face src ./src
USER face

EXPOSE 8084
HEALTHCHECK --interval=30s --timeout=5s --start-period=300s --retries=5 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8084/health', timeout=3)"
CMD ["python", "-m", "uvicorn", "src.face_auth.main:app", "--host", "0.0.0.0", "--port", "8084", "--workers", "1", "--no-access-log"]
