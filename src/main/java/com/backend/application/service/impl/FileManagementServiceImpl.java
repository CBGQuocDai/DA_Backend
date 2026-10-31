package com.backend.application.service.impl;

import com.backend.application.service.FileManagementService;
import com.backend.application.adapter.storage.StorageService;
import com.backend.application.adapter.file.FileDetector;
import com.backend.application.dto.request.upload.UploadUrlRequest;
import com.backend.application.dto.response.upload.UploadUrlResponse;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.valueobject.BusinessError;
import com.backend.domain.valueobject.UploadType;
import com.backend.application.adapter.storage.SignedUpload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileManagementServiceImpl implements FileManagementService {

    private static final long UPLOAD_URL_TTL_MINUTES = 15L;
    // Tika identifies a format from the start of the file, no need to download all of it
    private static final int DETECTION_HEAD_BYTES = 64 * 1024;

    private final StorageService storageService;
    private final FileDetector fileDetector;

    @Override
    public UploadUrlResponse createUploadUrl(UploadUrlRequest request) {
        UploadType type = request.getType();
        String extension = extensionOf(request.getFileName());
        List<String> contentTypes = type.getContentTypesByExtension().get(extension);
        if (contentTypes == null) {
            throw new BusinessException(type.getFormatError());
        }
        if (request.getSize() > type.getMaxSize()) {
            throw new BusinessException(type.getSizeError());
        }

        String filePath = UploadType.TEMP_PREFIX + type.newPath(extension);
        SignedUpload upload = storageService.createUploadSignUrl(
                filePath, contentTypes.getFirst(), type.getMaxSize(), UPLOAD_URL_TTL_MINUTES);
        return UploadUrlResponse.builder()
                .uploadUrl(upload.url())
                .filePath(filePath)
                .headers(upload.headers())
                .build();
    }

    @Override
    public void verifyUpload(String tempPath, UploadType type) {
        if (!tempPath.startsWith(UploadType.TEMP_PREFIX + type.getPathPrefix())) {
            throw new BusinessException(BusinessError.UPLOAD_NOT_FOUND);
        }
        long size = storageService.findSize(tempPath)
                .orElseThrow(() -> new BusinessException(BusinessError.UPLOAD_NOT_FOUND));
        if (size > type.getMaxSize()) {
            rejectUpload(tempPath, type.getSizeError());
        }
        String detected = fileDetector.detectContentType(storageService.loadHead(tempPath, DETECTION_HEAD_BYTES));
        List<String> contentTypes = type.getContentTypesByExtension().get(extensionOf(tempPath));
        if (contentTypes == null || !contentTypes.contains(detected)) {
            rejectUpload(tempPath, type.getFormatError());
        }
    }

    @Override
    public void moveToPermanent(String tempPath, UploadType type) {
        storageService.move(tempPath, type.permanentPath(tempPath));
    }

    private void rejectUpload(String tempPath, BusinessError error) {
        try {
            storageService.delete(tempPath);
        } catch (IOException e) {
            // The bucket lifecycle rule removes it later
            log.warn("Failed to delete rejected upload {}", tempPath, e);
        }
        throw new BusinessException(error);
    }

    // "Bìa.PNG" -> "png", no extension -> ""
    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
