"""Các job bị backend hủy (ví dụ khi admin gộp chương), nhận qua exchange fanout `tts.cancel`."""
import threading
import time


class CancelledJobs:
    """Tập jobId đã hủy. Lệnh hủy đến trên thread của connection, còn job chạy ở thread riêng nên cần khóa."""

    def __init__(self, ttl_seconds: float = 24 * 3600, clock=time.monotonic):
        self._ttl = ttl_seconds
        self._clock = clock
        self._cancelled: dict[str, float] = {}
        self._lock = threading.Lock()

    def add(self, job_id: str) -> None:
        with self._lock:
            now = self._clock()
            # Job bị hủy chỉ cần nhớ đến khi nó chắc chắn đã rời hàng đợi
            self._cancelled = {k: t for k, t in self._cancelled.items() if now - t < self._ttl}
            self._cancelled[job_id] = now

    def __contains__(self, job_id: object) -> bool:
        if not job_id:
            return False
        with self._lock:
            added = self._cancelled.get(job_id)
            return added is not None and self._clock() - added < self._ttl
