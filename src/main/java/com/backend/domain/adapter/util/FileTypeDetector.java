package com.backend.domain.adapter.util;

import org.springframework.web.multipart.MultipartFile;

/**
 * Detects whether an uploaded file's bytes actually represent a given media
 * category (audio, image, video, ...). Implementations live in the
 * infrastructure layer and may use libraries such as Apache Tika.
 */
public interface FileTypeDetector {
    /**
     * @return the MIME type detected from the file's bytes, or {@code null} when it cannot be identified.
     */
    String detectContentType(MultipartFile file);
}
