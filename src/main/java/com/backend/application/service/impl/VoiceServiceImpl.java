package com.backend.application.service.impl;


import com.backend.application.service.FileManagementService;
import com.backend.application.service.VoiceService;
import com.backend.application.adapter.audio.AudioConverter;
import com.backend.application.adapter.repository.VoiceRepository;
import com.backend.application.adapter.storage.StorageService;
import com.backend.application.dto.request.voice.VoiceRequest;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.voice.VoiceResponse;
import com.backend.application.mapper.ResponseMapper;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Voice;
import com.backend.domain.valueobject.BusinessError;
import com.backend.domain.valueobject.UploadType;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceServiceImpl implements VoiceService {

    private static final String MP3_TYPE = "mp3";
    private static final String WAV_TYPE = "wav";
    private static final String WAV_CONTENT_TYPE = "audio/wav";

    private final VoiceRepository voiceRepository;
    private final StorageService storageService;
    private final FileManagementService fileManagementService;
    private final ResponseMapper responseMapper;
    private final AudioConverter audioConverter;

    /** MP3 samples end up as WAV (the TTS worker needs WAV), under a path of their own. */
    private String targetPath(String tempPath) {
        return isMp3(tempPath) ? UploadType.AUDIO.newPath(WAV_TYPE) : UploadType.AUDIO.permanentPath(tempPath);
    }

    /** Runs after saving: a failed conversion or move rolls the save back. */
    private void publishExampleAudio(String tempPath, String targetPath) {
        if (!isMp3(tempPath)) {
            fileManagementService.moveToPermanent(tempPath, UploadType.AUDIO);
            return;
        }
        storageService.store(audioConverter.mp3ToWav(storageService.load(tempPath)), targetPath, WAV_CONTENT_TYPE);
        deleteQuietly(tempPath);
    }

    // The extension can be trusted: verifyUpload matched it against the file's bytes
    private boolean isMp3(String path) {
        return path.endsWith("." + MP3_TYPE);
    }

    private void deleteQuietly(String filePath) {
        try {
            storageService.delete(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete voice sample {}", filePath, e);
        }
    }

    @Override
    public PageResponse<VoiceResponse> getListVoice(String voiceName, int page, int size) {
        return responseMapper.toPageResponse(
                voiceRepository.findAllByNameLike(voiceName, page, size), responseMapper::toVoiceResponse);
    }

    @Override
    @Transactional
    public VoiceResponse createVoice(VoiceRequest req) {
        String tempPath = req.getExampleAudio();
        fileManagementService.verifyUpload(tempPath, UploadType.AUDIO);

        Voice v = new Voice();
        applyRequest(v, req, targetPath(tempPath));
        Voice saved = voiceRepository.save(v);
        publishExampleAudio(tempPath, saved.getExampleAudio());
        return responseMapper.toVoiceResponse(saved);
    }

    @Override
    @Transactional
    public VoiceResponse updateVoice(Long voiceId, VoiceRequest req) {
        Voice existing = voiceRepository.findById(voiceId)
                .orElseThrow(() -> new BusinessException(BusinessError.NOT_FOUND));
        String oldExampleAudio = existing.getExampleAudio();
        String requested = req.getExampleAudio();
        boolean newSample = !requested.equals(oldExampleAudio);

        if (newSample) {
            fileManagementService.verifyUpload(requested, UploadType.AUDIO);
        }
        applyRequest(existing, req, newSample ? targetPath(requested) : oldExampleAudio);
        Voice updated = voiceRepository.update(existing);
        if (newSample) {
            publishExampleAudio(requested, updated.getExampleAudio());
            if (oldExampleAudio != null) {
                deleteQuietly(oldExampleAudio);
            }
        }
        return responseMapper.toVoiceResponse(updated);
    }

    private void applyRequest(Voice voice, VoiceRequest req, String exampleAudio) {
        voice.setName(req.getName());
        voice.setDescription(req.getDescription());
        voice.setExampleAudio(exampleAudio);
    }

    @Override
    public void deleteVoice(Long voiceId) {
        voiceRepository.deleteById(voiceId);
    }
}
