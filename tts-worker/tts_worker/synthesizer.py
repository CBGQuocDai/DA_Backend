import logging
from dataclasses import dataclass

import soundfile as sf
import torch

log = logging.getLogger(__name__)


@dataclass(frozen=True)
class Synthesized:
    duration: int  # giây


class OmniVoiceSynthesizer:
    def __init__(self, model_name: str, device: str | None = None, language: str | None = None):
        from omnivoice.models.omnivoice import OmniVoice
        from omnivoice.utils.common import get_best_device

        device = device or get_best_device()
        self._language = language
        log.info("Loading %s on %s ...", model_name, device)
        dtype = torch.float16 if device != "cpu" else torch.float32
        self._model = OmniVoice.from_pretrained(model_name, device_map=device, dtype=dtype)

    def synthesize(self, text: str, speed: float, ref_audio: str | None, out_path: str) -> Synthesized:
        wav = self._model.generate(
            text=text, language=self._language, ref_audio=ref_audio, speed=speed
        )[0]
        sr = self._model.sampling_rate
        sf.write(out_path, wav, sr)
        return Synthesized(duration=int(round(wav.shape[-1] / sr)))
