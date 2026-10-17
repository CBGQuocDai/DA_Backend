package com.backend.application.validate;

import com.backend.domain.adapter.util.FileTypeDetector;
import com.backend.domain.dto.request.voice.VoiceCreateRequest;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.valueobject.BusinessError;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import java.util.Set;

/**
 * Centralised validator for voice-related use cases. Holds every rule that a
 * voice request, payload, or uploaded file must satisfy so the service layer
 * stays focused on orchestration.
 */
@Component
@RequiredArgsConstructor
public class VoiceValidator {

    private static final int MAX_VOICE_FIELD_LENGTH = 255;
    private static final long MAX_FILE_SIZE_FOR_VOICE = 10L * 1024L * 1024L; // 10 MB
    private static final String AUDIO_PREFIX = "audio/";

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("mp3", "wav");

    private final FileTypeDetector fileTypeDetector;

    /** Full validation for the create flow. */
    public void validateForCreate(VoiceCreateRequest req) {
        validateName(req.getName());
        validateDescription(req.getDescription());
    }

    /** Partial validation for the update flow — only fields actually supplied are checked. */
    public void validateForUpdate(VoiceCreateRequest req) {
        if (req.getName() != null) {
            validateName(req.getName());
        }
        if (req.getDescription() != null) {
            validateDescription(req.getDescription());
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(BusinessError.VOICE_NAME_REQUIRED);
        }
        if (name.length() > MAX_VOICE_FIELD_LENGTH) {
            throw new BusinessException(BusinessError.VOICE_NAME_TOO_LONG);
        }
    }

    private void validateDescription(String description) {
        if (description != null && description.length() > MAX_VOICE_FIELD_LENGTH) {
            throw new BusinessException(BusinessError.VOICE_DESCRIPTION_TOO_LONG);
        }
    }

    /**
     * Validate the uploaded sample audio: must exist, actually be audio by
     * byte sniffing, respect the size cap, and have an allowed extension.
     */
    public void validateAudioFile(MultipartFile file, String extension) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(BusinessError.FILE_FORMAT_INCORRECT);
        }
        if (!isAudio(file)) {
            throw new BusinessException(BusinessError.FILE_FORMAT_INCORRECT);
        }
        if (file.getSize() > MAX_FILE_SIZE_FOR_VOICE) {
            throw new BusinessException(BusinessError.VOICE_TOO_LARGE);
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(BusinessError.INVALID_AUDIO_EXTENSION);
        }
    }

    private boolean isAudio(MultipartFile file) {
        String mimeType = fileTypeDetector.detectAudioContentType(file);
        return Objects.nonNull(mimeType) && mimeType.startsWith(AUDIO_PREFIX);
    }
}
