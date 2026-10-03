package com.backend.domain.adapter.textToSpeech;

import com.backend.domain.dto.request.book.chapter.GenerateAudioRequest;

public interface TextToSpeechService {
    String generateAudio(GenerateAudioRequest request);
}
