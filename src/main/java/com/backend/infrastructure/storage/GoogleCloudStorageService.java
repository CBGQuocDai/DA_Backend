package com.backend.infrastructure.storage;

import com.backend.domain.adapter.storage.StorageService;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class GoogleCloudStorageService implements StorageService {

    private final Storage storage;

    @Value("${gcs.bucket:}")
    private String bucket;

    public GoogleCloudStorageService() {
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    @Override
    @Async
    public void store(MultipartFile file, String filePath) {
        if (storage == null) {
            log.warn("Storage is disabled or not configured. Skipping upload for {}", filePath);
            return;
        }
        BlobId blobId = BlobId.of(bucket, filePath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();
        try {
            storage.create(blobInfo, file.getInputStream());
        } catch (IOException e) {
            log.error("Have error when upload file : {}", e.getMessage());
        }
    }

    @Override
    public String createSignUrl(String filePath, Long minutes) {
        if (filePath == null || filePath.isBlank()) {
            return "";
        }
        if (storage == null) {
            log.debug("Storage is disabled. Returning raw file path: {}", filePath);
            return filePath;
        }
        BlobId blobId = BlobId.of(bucket, filePath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        try {
            return storage.signUrl(blobInfo, minutes, TimeUnit.MINUTES).toString();
        } catch (StorageException e) {
            log.error("Failed to generate signed URL for {}: {}", filePath, e.getMessage());
            return filePath;
        }
    }

    @Override
    public Byte[] load(String path) {
        return new Byte[0];
    }
}
