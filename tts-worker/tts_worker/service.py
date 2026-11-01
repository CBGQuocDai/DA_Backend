import logging
import os
import tempfile
import uuid

from .config import Config
from .text import normalize_text
from .messages import TtsJob, TtsResult
from .storage import GcsStorage
from .synthesizer import OmniVoiceSynthesizer

log = logging.getLogger(__name__)


class TtsService:
    """Xử lý một job: tải giọng mẫu → sinh audio → upload → dựng kết quả."""

    def __init__(self, config: Config, storage: GcsStorage, synthesizer: OmniVoiceSynthesizer):
        self._config = config
        self._storage = storage
        self._synthesizer = synthesizer
    def handle(self, body: bytes) -> TtsResult | None:
        """None nếu message hỏng (không biết chapterId để báo lại)."""
        try:
            job = TtsJob.parse(body)
        except (ValueError, KeyError, TypeError):
            log.exception("Job không hợp lệ, bỏ qua: %r", body[:200])
            return None
        try:
            log.info("Job chapter %s: %d ký tự", job.chapter_id, len(job.text))
            log.info("Text: %r", job.text[:200])
            result = self._process(job)
            log.info("Chapter %s xong: %s (%ss)", job.chapter_id, result.audio_path, result.duration)
            return result
        except Exception as e:
            log.exception("Chapter %s lỗi", job.chapter_id)
            return TtsResult(job.chapter_id, False, error=f"{type(e).__name__}: {e}")

    def _process(self, job: TtsJob) -> TtsResult:
        if not job.text:
            raise ValueError("text rỗng")
        with tempfile.TemporaryDirectory() as tmp:
            ref_audio = None
            if job.example_voice_url:
                ref_audio = os.path.join(tmp, "ref" + os.path.splitext(job.example_voice_url)[1])
                self._storage.download(job.example_voice_url, ref_audio)

            out = os.path.join(tmp, "out.wav")
            audio = self._synthesizer.synthesize(normalize_text(job.text), job.speed, ref_audio, out)

            path = f"{self._config.audio_prefix}/{job.chapter_id}/{uuid.uuid4().hex}.wav"
            self._storage.upload(out, path, "audio/wav")
        return TtsResult(job.chapter_id, True, audio_path=path, duration=audio.duration)
