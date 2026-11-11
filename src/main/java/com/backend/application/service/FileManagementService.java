package com.backend.application.service;

import com.backend.application.dto.request.upload.UploadUrlRequest;
import com.backend.application.dto.response.upload.UploadUrlResponse;
import com.backend.domain.valueobject.UploadType;

public interface FileManagementService {

    UploadUrlResponse createUploadUrl(UploadUrlRequest request);

    void verifyUpload(String tempPath, UploadType type);

    void moveToPermanent(String tempPath, UploadType type);
}
