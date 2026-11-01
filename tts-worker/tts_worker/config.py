import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Config:
    rabbitmq_host: str
    rabbitmq_port: int
    rabbitmq_username: str
    rabbitmq_password: str
    gcs_bucket: str
    model: str
    audio_prefix: str
    device: str | None
    language: str | None

    @classmethod
    def from_env(cls) -> "Config":
        return cls(
            rabbitmq_host=os.getenv("RABBITMQ_HOST", "localhost"),
            rabbitmq_port=int(os.getenv("RABBITMQ_PORT", "5672")),
            rabbitmq_username=os.getenv("RABBITMQ_USERNAME", "guest"),
            rabbitmq_password=os.getenv("RABBITMQ_PASSWORD", "guest"),
            gcs_bucket=os.environ["GCS_BUCKET"],
            model=os.getenv("OMNIVOICE_MODEL", "k2-fsa/OmniVoice"),
            audio_prefix=os.getenv("AUDIO_PREFIX", "chapter-audio").strip("/"),
            device=os.getenv("DEVICE") or None,
            language=os.getenv("LANGUAGE", "vi") or None,  # để trống = model tự đoán
        )
