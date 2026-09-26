package com.backend.application.impl;


import com.backend.application.VoiceService;
import com.backend.domain.adapter.repository.VoiceRepository;
import com.backend.domain.adapter.storage.StorageService;
import com.backend.domain.dto.request.voice.VoiceCreateRequest;
import com.backend.domain.dto.response.PageResponse;
import com.backend.domain.mapper.VoiceMapper;
import com.backend.domain.model.Voice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class VoiceServiceImpl implements VoiceService {

    private final VoiceRepository voiceRepository;
    private final ObjectMapper objectMapper;
    private final StorageService storageService;
    private final VoiceMapper voiceMapper;

    private String VOICE_EXAMPLE_PATH = "/voice-example/";
    @Override
    public PageResponse<Voice> getListVoice() {
        return voiceRepository.findAll(0,10);
    }

    @Override
    public Voice createVoice(String voiceInfo, MultipartFile exampleAudio) {
        VoiceCreateRequest req;
        try {
            req = objectMapper.readValue(voiceInfo, VoiceCreateRequest.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse voiceInfo", e);
        }
        //        TODO: validate type of file
        String fileName = exampleAudio.getOriginalFilename();
        String filePath = VOICE_EXAMPLE_PATH + fileName;
        storageService.store(exampleAudio, filePath);
        Voice v = Voice.builder()
                .name(req.getName())
                .description(req.getDescription())
                .exampleAudio(filePath)
                .build();
        return voiceRepository.save(v);
    }

    @Override
    public Voice updateVoice(Long voiceId, Voice v) {
        return null;
    }

    @Override
    public void deleteVoice(Long voiceId) {

    }
}
