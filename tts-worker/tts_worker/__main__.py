import logging

from dotenv import load_dotenv

from .config import Config
from .consumer import RabbitMqConsumer
from .service import TtsService
from .storage import GcsStorage
from .synthesizer import OmniVoiceSynthesizer


def main() -> None:
    logging.basicConfig(level=logging.INFO,
                        format="%(asctime)s %(levelname)s [%(name)s] %(message)s")
    load_dotenv()  # đọc .env nếu có; biến môi trường thật vẫn được ưu tiên
    config = Config.from_env()
    service = TtsService(
        config,
        GcsStorage(config.gcs_bucket),
        OmniVoiceSynthesizer(config.model, config.device, config.language),
    )
    RabbitMqConsumer(config, service).run_forever()


if __name__ == "__main__":
    main()
