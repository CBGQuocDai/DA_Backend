package com.backend.presentation.controller;

import com.backend.application.dto.request.progress.ListenProgressRequest;
import com.backend.application.dto.response.progress.BookProgressResponse;
import com.backend.application.service.ListenProgressService;
import com.backend.presentation.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customer/books/{bookId}")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class ListenProgressController {
    private final ListenProgressService service;

    // Lấy toàn bộ tiến độ nghe của khách hàng đối với cuốn sách (vị trí từng chương, điểm tiếp tục phát)
    @GetMapping("/progress")
    public ResponseEntity<ApiResponse<BookProgressResponse>> getProgress(@AuthenticationPrincipal Long customerId,
                                                                       @PathVariable Long bookId) {
        return ResponseEntity.ok(ApiResponse.<BookProgressResponse>builder()
                .data(service.getProgress(customerId, bookId)).build());
    }

    // Lưu hoặc cập nhật tiến độ nghe (giây hiện tại, trạng thái hoàn thành) cho một chương
    @PutMapping("/chapters/{chapterId}/progress")
    public ResponseEntity<ApiResponse<BookProgressResponse>> saveProgress(@AuthenticationPrincipal Long customerId,
            @PathVariable Long bookId, @PathVariable Long chapterId, @Valid @RequestBody ListenProgressRequest request) {
        return ResponseEntity.ok(ApiResponse.<BookProgressResponse>builder()
                .data(service.saveProgress(customerId, bookId, chapterId, request)).build());
    }
}
