package com.backend.infrastructure.tts;

import com.backend.domain.adapter.textToSpeech.TextToSpeechService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OmniVoiceService implements TextToSpeechService {
    @Override
    public Map<String, Object> generateAudio() {
        return Map.of();
    }
}
