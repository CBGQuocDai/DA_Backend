
package com.backend.infrastructure.storage;

import com.backend.application.adapter.storage.StorageService;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.valueobject.BusinessError;
import com.backend.application.adapter.storage.SignedUpload;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class GoogleCloudStorageService implements StorageService {

    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    // GCS rejects the PUT when the body size is outside "min,max"
    private static final String CONTENT_LENGTH_RANGE_HEADER = "x-goog-content-length-range";

    private final Storage storage;

    @Value("${gcs.bucket:}")
    private String bucket;

    public GoogleCloudStorageService() {
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    @Override
    public void store(byte[] content, String filePath, String contentType) {
        BlobId blobId = BlobId.of(bucket, filePath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();
        try {
            storage.create(blobInfo, content);
        } catch (StorageException e) {
            log.error("Have error when upload file {}: {}", filePath, e.getMessage());
            throw new BusinessException(BusinessError.ERROR_NOT_DEFINE);
        }
    }

    @Override
    public String createSignUrl(String filePath, Long minutes) {
        if (filePath == null || filePath.isBlank()) {
            return "";
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
    public SignedUpload createUploadSignUrl(String filePath, String contentType, long maxSize, long minutes) {
        Map<String, String> extHeaders = Map.of(CONTENT_LENGTH_RANGE_HEADER, "1," + maxSize);
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucket, filePath))
                .setContentType(contentType)
                .build();
        try {
            String url = storage.signUrl(blobInfo, minutes, TimeUnit.MINUTES,
                    Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                    Storage.SignUrlOption.withContentType(),
                    Storage.SignUrlOption.withExtHeaders(extHeaders),
                    Storage.SignUrlOption.withV4Signature()).toString();
            return new SignedUpload(url, Map.of(
                    CONTENT_TYPE_HEADER, contentType,
                    CONTENT_LENGTH_RANGE_HEADER, extHeaders.get(CONTENT_LENGTH_RANGE_HEADER)));
        } catch (StorageException | IllegalStateException e) {
            log.error("Failed to generate upload signed URL for {}: {}", filePath, e.getMessage());
            throw new BusinessException(BusinessError.ERROR_NOT_DEFINE);
        }
    }

    @Override
    public byte[] load(String filePath) {
        try {
            return storage.readAllBytes(BlobId.of(bucket, filePath));
        } catch (StorageException e) {
            log.error("Failed to download {}: {}", filePath, e.getMessage());
            throw new BusinessException(BusinessError.ERROR_NOT_DEFINE);
        }
    }

    @Override
    public byte[] loadHead(String filePath, int maxBytes) {
        try (ReadChannel reader = storage.reader(BlobId.of(bucket, filePath))) {
            reader.limit(maxBytes);
            ByteBuffer buffer = ByteBuffer.allocate(maxBytes);
            while (buffer.hasRemaining() && reader.read(buffer) > 0) {
                // A single read may return less than requested
            }
            return Arrays.copyOf(buffer.array(), buffer.position());
        } catch (IOException | StorageException e) {
            log.error("Failed to read the head of {}: {}", filePath, e.getMessage());
            throw new BusinessException(BusinessError.ERROR_NOT_DEFINE);
        }
    }

    @Override
    public Optional<Long> findSize(String filePath) {
        try {
            return Optional.ofNullable(storage.get(BlobId.of(bucket, filePath))).map(Blob::getSize);
        } catch (StorageException e) {
            log.error("Failed to read metadata of {}: {}", filePath, e.getMessage());
            throw new BusinessException(BusinessError.ERROR_NOT_DEFINE);
        }
    }

    @Override
    public void move(String sourcePath, String targetPath) {
        BlobId source = BlobId.of(bucket, sourcePath);
        try {
            storage.copy(Storage.CopyRequest.of(source, BlobId.of(bucket, targetPath))).getResult();
            storage.delete(source);
        } catch (StorageException e) {
            log.error("Failed to move {} to {}: {}", sourcePath, targetPath, e.getMessage());
            throw new BusinessException(BusinessError.ERROR_NOT_DEFINE);
        }
    }

    @Override
    public void delete(String filePath) throws IOException {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        try {
            BlobId blobId = BlobId.of(bucket, filePath);
            boolean deleted = storage.delete(blobId);
            if (!deleted) {
                log.debug("File không tồn tại trên GCS hoặc đã bị xóa trước đó: {}", filePath);
            }
        } catch (StorageException e) {
            log.error("Lỗi khi xóa file trên GCS {}: {}", filePath, e.getMessage());
            throw new IOException("Không xóa được file trên GCS: " + filePath, e);
        }
    }
}
