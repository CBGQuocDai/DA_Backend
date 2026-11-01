FROM nvidia/cuda:12.8.1-runtime-ubuntu24.04

ENV DEBIAN_FRONTEND=noninteractive \
    PYTHONUNBUFFERED=1 \
    HF_HOME=/models

RUN apt-get update && apt-get install -y --no-install-recommends \
        python3 python3-venv ffmpeg libsndfile1 \
    && rm -rf /var/lib/apt/lists/*

RUN python3 -m venv /opt/venv
ENV PATH=/opt/venv/bin:$PATH

# torch build CUDA 12.8 trước, để omnivoice không kéo bản khác
RUN pip install --no-cache-dir torch==2.11.0 torchaudio==2.11.0 \
        --index-url https://download.pytorch.org/whl/cu128

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY tts_worker ./tts_worker

# cache model HuggingFace; mount volume để khỏi tải lại mỗi lần chạy
VOLUME /models

CMD ["python", "-m", "tts_worker"]
