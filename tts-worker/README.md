# TTS worker (OmniVoice)

Consume `tts.jobs` → sinh audio → upload GCS → publish `tts.results`.
Định dạng message: `docs/superpowers/specs/2026-10-29-tts-async-queue-design.md`.

## Chạy local
```bash
source ~/.venv/bin/activate
pip install -r requirements.txt   # omnivoice/torch đã có thì bỏ qua
cp .env.example .env   # điền giá trị
python -m tts_worker   # tự đọc .env
```

## Docker (cần NVIDIA Container Toolkit)
```bash
docker build -t tts-worker .
docker run --rm --gpus all --env-file .env \
  -v $PWD/gcp.json:/secrets/gcp.json:ro \
  -v hf-cache:/models \
  tts-worker
```
Nếu RabbitMQ chạy trên host, đặt `RABBITMQ_HOST=host.docker.internal` và thêm `--add-host=host.docker.internal:host-gateway`.
