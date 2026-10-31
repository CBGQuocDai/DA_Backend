package com.backend.presentation.controller;

import com.backend.application.service.FileManagementService;
import com.backend.application.dto.request.upload.UploadUrlRequest;
import com.backend.presentation.dto.ApiResponse;
import com.backend.application.dto.response.upload.UploadUrlResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Frontend uploads files straight to cloud storage: it asks here for a signed
 * URL, PUTs the file there, then sends the returned temporary path in the
 * book/voice request, which verifies the file and moves it out of tmp/.
 */
@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final FileManagementService fileManagementService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/sign-url")
    public ResponseEntity<ApiResponse<UploadUrlResponse>> createUploadUrl(
            @Valid @RequestBody UploadUrlRequest request) {
        return ResponseEntity.ok(ApiResponse.<UploadUrlResponse>builder()
                .data(fileManagementService.createUploadUrl(request))
                .build());
    }
}
