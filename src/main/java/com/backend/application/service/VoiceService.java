package com.backend.application.service;

import com.backend.domain.dto.response.PageResponse;
import com.backend.domain.model.Voice;
import org.springframework.web.multipart.MultipartFile;

public interface VoiceService {
    PageResponse<Voice> getListVoice(String voiceName, int page, int size);
    Voice createVoice(String voiceInfo, MultipartFile exampleAudio);
    Voice updateVoice(Long voiceId, String voiceInfo, MultipartFile exampleAudio);
    void deleteVoice(Long voiceId);
}
