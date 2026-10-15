package com.backend.application.service.impl;


import com.backend.application.service.VoiceService;
import com.backend.application.validate.VoiceValidator;
import com.backend.domain.adapter.repository.VoiceRepository;
import com.backend.domain.adapter.storage.StorageService;
import com.backend.domain.dto.request.voice.VoiceCreateRequest;
import com.backend.domain.dto.response.PageResponse;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Voice;
import com.backend.domain.valueobject.BusinessError;
import com.backend.infrastructure.util.InMemoryMultipartFile;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.info.MultimediaInfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceServiceImpl implements VoiceService {

    private static final String VOICE_EXAMPLE_PATH = "voice-example/";

    private static final String MP3_TYPE = "mp3";
    private static final String WAV_TYPE = "wav";

    private final VoiceRepository voiceRepository;
    private final ObjectMapper objectMapper;
    private final StorageService storageService;
    private final VoiceValidator voiceValidator;

    private String getExtensionFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "";
        }

        String fileName = file.getOriginalFilename();
        if (!StringUtils.hasText(fileName)) {
            return "";
        }

        int dotIndex = fileName.lastIndexOf('.');

        // dotIndex <= 0: Tránh file không có dấu chấm hoặc file ẩn dạng ".gitignore"
        // dotIndex == fileName.length() - 1: Tránh trường hợp đuôi kết thúc bằng dấu chấm "audio."
        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    private MultipartFile convertMp3ToWav(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String baseName = originalName != null && originalName.contains(".")
                ? originalName.substring(0, originalName.lastIndexOf('.'))
                : UUID.randomUUID().toString();
        String wavName = baseName + "_" + UUID.randomUUID() + "." + WAV_TYPE;

        Path source = null;
        Path target = null;
        try {
            source = Files.createTempFile("voice-src-", "." + MP3_TYPE);
            target = Files.createTempFile("voice-out-", "." + WAV_TYPE);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, source, StandardCopyOption.REPLACE_EXISTING);
            }

            MultimediaObject sourceObj = new MultimediaObject(source.toFile());
            MultimediaInfo info = sourceObj.getInfo();
            int channels = (info.getAudio() != null && info.getAudio().getChannels() > 0)
                    ? info.getAudio().getChannels()
                    : 1;
            int sampleRate = (info.getAudio() != null && info.getAudio().getSamplingRate() > 0)
                    ? info.getAudio().getSamplingRate()
                    : 44100;

            AudioAttributes audioAttrs = new AudioAttributes();
            audioAttrs.setCodec("pcm_s16le");
            audioAttrs.setChannels(channels);
            audioAttrs.setSamplingRate(sampleRate);
            audioAttrs.setBitRate(256_000);

            EncodingAttributes encodingAttrs = new EncodingAttributes();
            encodingAttrs.setOutputFormat(WAV_TYPE);
            encodingAttrs.setAudioAttributes(audioAttrs);

            new Encoder().encode(sourceObj, target.toFile(), encodingAttrs);

            byte[] wavBytes = Files.readAllBytes(target);
            return new InMemoryMultipartFile(wavBytes, wavName, "audio/wav");
        } catch (EncoderException | IOException e) {
            log.error("Failed to convert mp3 to wav", e);
            throw new BusinessException(BusinessError.FILE_FORMAT_INCORRECT);
        } finally {
            deleteIfExists(source);
            deleteIfExists(target);
        }
    }

    private void deleteIfExists(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete temp file {}", path, e);
        }
    }

    private VoiceCreateRequest parseVoiceInfo(String voiceInfo) {
        if (voiceInfo == null || voiceInfo.isBlank()) {
            throw new BusinessException(BusinessError.MALFORMED_JSON);
        }
        try {
            return objectMapper.readValue(voiceInfo, VoiceCreateRequest.class);
        } catch (Exception e) {
            log.warn("Failed to parse voiceInfo: {}", e.getMessage());
            throw new BusinessException(BusinessError.MALFORMED_JSON);
        }
    }

    @Override
    public PageResponse<Voice> getListVoice(String voiceName, int page, int size) {
        PageResponse<Voice> v = voiceRepository.findAllByNameLike(voiceName, page, size);
        v.getContent().forEach(voice -> 
           voice.setExampleAudio(storageService.createSignUrl(voice.getExampleAudio(), 60L))
        );
        return v;
    }

    @Override
    public Voice createVoice(String voiceInfo, MultipartFile exampleAudio) {
        VoiceCreateRequest req = parseVoiceInfo(voiceInfo);
        voiceValidator.validateForCreate(req);

        String extension = getExtensionFile(exampleAudio);
        voiceValidator.validateAudioFile(exampleAudio, extension);
        if(MP3_TYPE.equals(extension)) {
            exampleAudio = convertMp3ToWav(exampleAudio);
        }
        String filePath = VOICE_EXAMPLE_PATH + exampleAudio.getOriginalFilename();
        storageService.store(exampleAudio, filePath);
        Voice v = Voice.builder()
                .name(req.getName())
                .description(req.getDescription())
                .exampleAudio(filePath)
                .build();
        return voiceRepository.save(v);
    }

    @Override
    @Transactional
    public Voice updateVoice(Long voiceId, String voiceInfo, MultipartFile exampleAudio) {
        Voice existing = voiceRepository.findById(voiceId)
                .orElseThrow(() -> new BusinessException(BusinessError.NOT_FOUND));

        if (voiceInfo != null && !voiceInfo.isBlank()) {
            VoiceCreateRequest req = parseVoiceInfo(voiceInfo);
            voiceValidator.validateForUpdate(req);
            if (req.getName() != null) {
                existing.setName(req.getName());
            }
            if (req.getDescription() != null) {
                existing.setDescription(req.getDescription());
            }
        }

        if (exampleAudio != null && !exampleAudio.isEmpty()) {
            String extension = getExtensionFile(exampleAudio);
            voiceValidator.validateAudioFile(exampleAudio, extension);
            if (MP3_TYPE.equals(extension)) {
                exampleAudio = convertMp3ToWav(exampleAudio);
            }
            String filePath = VOICE_EXAMPLE_PATH + exampleAudio.getOriginalFilename();
            storageService.store(exampleAudio, filePath);
            existing.setExampleAudio(filePath);
        }
        Voice updated = voiceRepository.update(existing);
        if (updated.getExampleAudio() != null) {
            updated.setExampleAudio(storageService.createSignUrl(updated.getExampleAudio(), 60L));
        }
        return updated;
    }

    @Override
    public void deleteVoice(Long voiceId) {
        voiceRepository.deleteById(voiceId);
    }
}
