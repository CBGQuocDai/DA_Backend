package com.backend.domain.adapter.util;

import org.springframework.web.multipart.MultipartFile;

/**
 * Detects whether an uploaded file's bytes actually represent a given media
 * category (audio, image, video, ...). Implementations live in the
 * infrastructure layer and may use libraries such as Apache Tika.
 */
public interface FileTypeDetector {
    /**
     * @return the detected audio MIME type, or {@code null} when the file is not audio
     *         (or cannot be identified).
     */
    String detectAudioContentType(MultipartFile file);
}