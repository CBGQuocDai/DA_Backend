"""Định dạng message, khớp GenerateAudioRequest / GenerateAudioResult của backend."""
import json
from dataclasses import asdict, dataclass


@dataclass(frozen=True)
class TtsJob:
    chapter_id: int
    text: str
    example_voice_url: str
    speed: float

    @classmethod
    def parse(cls, body: bytes) -> "TtsJob":
        data = json.loads(body)
        return cls(
            chapter_id=data["chapterId"],
            text=(data.get("text") or "").strip(),
            example_voice_url=data.get("exampleVoiceUrl") or "",
            speed=data.get("speed") or 1.0,
        )


@dataclass(frozen=True)
class TtsResult:
    chapter_id: int
    success: bool
    audio_path: str | None = None
    duration: int | None = None
    error: str | None = None

    def to_json(self) -> bytes:
        d = asdict(self)
        payload = {
            "chapterId": d["chapter_id"], "success": d["success"],
            "audioPath": d["audio_path"], "duration": d["duration"],
            "error": d["error"],
        }
        return json.dumps(payload).encode("utf-8")
