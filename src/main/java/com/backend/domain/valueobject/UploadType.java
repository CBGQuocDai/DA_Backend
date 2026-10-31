package com.backend.domain.valueobject;

import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
public enum UploadType {
    IMAGE("images/", 5L * 1024L * 1024L,
            Map.of("jpg", List.of("image/jpeg"),
                    "jpeg", List.of("image/jpeg"),
                    "png", List.of("image/png"),
                    "webp", List.of("image/webp")),
            BusinessError.IMAGE_FORMAT_INCORRECT, BusinessError.IMAGE_TOO_LARGE),
    AUDIO("audio/", 10L * 1024L * 1024L,
            Map.of("mp3", List.of("audio/mpeg", "audio/mp3"),
                    "wav", List.of("audio/wav", "audio/x-wav", "audio/wave", "audio/vnd.wave")),
            BusinessError.AUDIO_FORMAT_INCORRECT, BusinessError.AUDIO_TOO_LARGE),
    PDF("pdfs/", 100L * 1024L * 1024L,
            Map.of("pdf", List.of("application/pdf")),
            BusinessError.PDF_FORMAT_INCORRECT, BusinessError.PDF_TOO_LARGE);

    // Files wait here until a book/voice claims them; a bucket lifecycle rule removes the abandoned ones
    public static final String TEMP_PREFIX = "tmp/";

    private final String pathPrefix;
    private final long maxSize;
    // The first content type of an extension is the one its uploads are signed with
    private final Map<String, List<String>> contentTypesByExtension;
    private final BusinessError formatError;
    private final BusinessError sizeError;

    UploadType(String pathPrefix, long maxSize, Map<String, List<String>> contentTypesByExtension,
               BusinessError formatError, BusinessError sizeError) {
        this.pathPrefix = pathPrefix;
        this.maxSize = maxSize;
        this.contentTypesByExtension = contentTypesByExtension;
        this.formatError = formatError;
        this.sizeError = sizeError;
    }

    public String newPath(String extension) {
        return pathPrefix + UUID.randomUUID() + "." + extension;
    }

    public String permanentPath(String tempPath) {
        return tempPath.substring(TEMP_PREFIX.length());
    }
}
