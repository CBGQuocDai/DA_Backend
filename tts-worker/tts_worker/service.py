import logging
import os
import tempfile
import uuid

from .cancellation import CancelledJobs
from .config import Config
from .text import normalize_text
from .messages import TtsJob, TtsResult
from .storage import GcsStorage
from .synthesizer import OmniVoiceSynthesizer

log = logging.getLogger(__name__)


class Cancelled:
    """Job bị backend hủy: không có kết quả nào để gửi lại."""


CANCELLED = Cancelled()


class _JobCancelled(Exception):
    pass


class TtsService:
    """Xử lý một job: tải giọng mẫu → sinh audio → upload → dựng kết quả."""

    def __init__(self, config: Config, storage: GcsStorage, synthesizer: OmniVoiceSynthesizer,
                 cancelled: CancelledJobs | None = None):
        self._config = config
        self._cancelled = cancelled if cancelled is not None else CancelledJobs()
        self._storage = storage
        self._synthesizer = synthesizer
    @property
    def cancelled(self) -> CancelledJobs:
        return self._cancelled

    def handle(self, body: bytes) -> TtsResult | Cancelled | None:
        """None nếu message hỏng (không biết chapterId để báo lại); CANCELLED nếu job đã bị hủy."""
        try:
            job = TtsJob.parse(body)
        except (ValueError, KeyError, TypeError):
            log.exception("Job không hợp lệ, bỏ qua: %r", body[:200])
            return None
        if job.job_id in self._cancelled:
            log.info("Job %s (chapter %s) đã bị hủy, bỏ qua", job.job_id, job.chapter_id)
            return CANCELLED
        try:
            log.info("Job chapter %s: %d ký tự", job.chapter_id, len(job.text))
            log.info("Text: %r", job.text[:200])
            result = self._process(job)
            log.info("Chapter %s xong: %s (%ss)", job.chapter_id, result.audio_path, result.duration)
            return result
        except _JobCancelled:
            log.info("Job %s (chapter %s) bị hủy trong lúc sinh audio, không upload", job.job_id, job.chapter_id)
            return CANCELLED
        except Exception as e:
            log.exception("Chapter %s lỗi", job.chapter_id)
            return TtsResult(job.chapter_id, False, error=f"{type(e).__name__}: {e}", job_id=job.job_id)

    def _process(self, job: TtsJob) -> TtsResult:
        if not job.text:
            raise ValueError("text rỗng")
        with tempfile.TemporaryDirectory() as tmp:
            ref_audio = None
            if job.example_voice_url:
                ref_audio = os.path.join(tmp, "ref" + os.path.splitext(job.example_voice_url)[1])
                self._storage.download(job.example_voice_url, ref_audio)

            out = os.path.join(tmp, "out.wav")
            # Chỉ dùng lời thoại khi có giọng mẫu; không có thì thư viện tự nhận dạng (ASR) và tự cắt mẫu >20s
            ref_text = job.example_voice_text if ref_audio else None
            audio = self._synthesizer.synthesize(normalize_text(job.text), job.speed, ref_audio, out, ref_text)
            # Không ngắt được model giữa chừng, nhưng tránh upload file mà backend sẽ bỏ đi
            if job.job_id in self._cancelled:
                raise _JobCancelled()

            path = f"{self._config.audio_prefix}/{job.chapter_id}/{uuid.uuid4().hex}.wav"
            self._storage.upload(out, path, "audio/wav")
        return TtsResult(job.chapter_id, True, audio_path=path, duration=audio.duration, job_id=job.job_id)
