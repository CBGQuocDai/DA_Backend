package com.backend.application;

import com.backend.domain.dto.request.voice.VoiceCreateRequest;
import com.backend.domain.dto.response.PageResponse;
import com.backend.domain.model.Voice;
import org.springframework.web.multipart.MultipartFile;

public interface VoiceService {
    PageResponse<Voice> getListVoice();
    Voice createVoice(String voiceInfo, MultipartFile exampleAudio);
    Voice updateVoice(Long voiceId, Voice v);
    void deleteVoice(Long voiceId);

}
