package com.backend.application.service;

import com.backend.application.dto.request.upload.UploadUrlRequest;
import com.backend.application.dto.response.upload.UploadUrlResponse;
import com.backend.domain.valueobject.UploadType;

public interface FileManagementService {

    /** Checks the declared metadata and signs a URL to upload the file under tmp/. */
    UploadUrlResponse createUploadUrl(UploadUrlRequest request);

    /**
     * Checks a file waiting under tmp/ before a book/voice claims it: it must be
     * of this type, exist, and its real size and bytes must be allowed. A file
     * that fails on size or format is deleted.
     */
    void verifyUpload(String tempPath, UploadType type);

    /** Moves a verified file to {@link UploadType#permanentPath}. */
    void moveToPermanent(String tempPath, UploadType type);
}
