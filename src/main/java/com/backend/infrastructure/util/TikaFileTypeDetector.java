package com.backend.infrastructure.util;

import com.backend.domain.adapter.util.FileTypeDetector;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Apache Tika-backed implementation of {@link FileTypeDetector}.
 *
 * <p>Tika inspects a file's magic bytes (independent of the declared extension
 * or {@code Content-Type} header) to determine the actual media type. The
 * file is treated as audio when the detected content-type starts with
 * {@code audio/}.</p>
 */
@Slf4j
@Component
public class TikaFileTypeDetector implements FileTypeDetector {


    private final Detector detector = new DefaultDetector();

    @Override
    public String detectContentType(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        Metadata metadata = new Metadata();
        if (file.getOriginalFilename() != null) {
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, file.getOriginalFilename());
        }
        try (TikaInputStream stream = TikaInputStream.get(file.getInputStream())) {
            MediaType mediaType = detector.detect(stream, metadata);
            return mediaType.toString();
        } catch (IOException e) {
            log.warn("Failed to detect media type for uploaded file", e);
            return null;
        }
    }
}