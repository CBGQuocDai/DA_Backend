package com.backend.infrastructure.tts;

import com.backend.domain.adapter.textToSpeech.TextToSpeechService;
import com.backend.domain.dto.request.book.chapter.GenerateAudioRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class OmniVoiceService implements TextToSpeechService {

    private final String ttsServiceUrl;

    private final RestClient restClient;

    public OmniVoiceService() {
        this.ttsServiceUrl = "https://instance-20261002-011002.tail01965e.ts.net";
        this.restClient = RestClient.builder().baseUrl(ttsServiceUrl).build();
    }

    @Override
    public String generateAudio(GenerateAudioRequest request) {
        log.info("Sending TTS request to OmniVoice service at {} with exampleVoiceUrl: {}",
                ttsServiceUrl, request.getExampleVoiceUrl());
        try {
            ResponseEntity<String> response = restClient.post()
                    .uri("/generate-audio")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toEntity(String.class);
            log.info("TTS response from OmniVoice service: {}", response.getBody());
            assert response.getBody() != null;
            return response.getBody().substring(2,  response.getBody().length() - 1);
        } catch (Exception e) {
            log.error("Failed to generate audio via OmniVoiceService: {}", e.getMessage(), e);
            throw new RuntimeException("OmniVoice audio generation failed: " + e.getMessage(), e);
        }
//        return "";
    }
}

