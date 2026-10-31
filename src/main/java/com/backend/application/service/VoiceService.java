package com.backend.application.service;

import com.backend.application.dto.request.voice.VoiceRequest;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.voice.VoiceResponse;

public interface VoiceService {
    PageResponse<VoiceResponse> getListVoice(String voiceName, int page, int size);
    VoiceResponse createVoice(VoiceRequest request);
    VoiceResponse updateVoice(Long voiceId, VoiceRequest request);
    void deleteVoice(Long voiceId);
}
