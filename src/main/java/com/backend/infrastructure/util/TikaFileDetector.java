package com.backend.infrastructure.util;

import com.backend.application.adapter.file.FileDetector;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class TikaFileDetector implements FileDetector {

    private final Detector detector = new DefaultDetector();

    @Override
    public String detectContentType(byte[] content) {
        // No file name in the metadata: Tika falls back to it when the magic bytes are unknown
        try (TikaInputStream stream = TikaInputStream.get(content)) {
            return detector.detect(stream, new Metadata()).getBaseType().toString();
        } catch (IOException e) {
            log.warn("Failed to detect media type of uploaded file", e);
            return MediaType.OCTET_STREAM.toString();
        }
    }
}
