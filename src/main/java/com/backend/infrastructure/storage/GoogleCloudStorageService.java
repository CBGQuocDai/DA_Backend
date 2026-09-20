package com.backend.infrastructure.storage;

import com.backend.domain.adapter.storage.StorageService;
import com.google.cloud.storage.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class GoogleCloudStorageService implements StorageService {

    private final Storage storage;

    @Value("${gcs.bucket}")
    private String bucket;

    public GoogleCloudStorageService() {
//        this.storage = StorageOptions.newBuilder()
//                .setCredentials(ServiceAccountCredentials.fromStream(
//                        new ClassPathResource(credentialsPath).getInputStream()))
//                .build()
//                .getService();
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    @Override
    public void store(MultipartFile file, String filePath) throws IOException {
        BlobId blobId = BlobId.of(bucket, filePath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();
        storage.create(blobInfo, file.getInputStream());
    }

    @Override
    public Byte[] load(String path) {
        return new Byte[0];
    }
}
