package com.backend.presentation.controller;

import com.backend.domain.valueobject.BusinessError;
import com.backend.domain.exception.BusinessException;
import com.backend.application.dto.request.book.chapter.ChapterUploadAudioRequest;
import com.backend.application.service.ChapterService;
import com.backend.application.dto.request.book.chapter.ChapterCreateRequest;
import com.backend.application.dto.request.book.chapter.ChapterGenerateAudioRequest;
import com.backend.application.dto.request.book.chapter.ChapterUpdateRequest;
import com.backend.presentation.dto.ApiResponse;
import com.backend.application.dto.response.chapter.ChapterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/books/{bookId}/chapters")
public class ChapterController {

    private final ChapterService chapterService;

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{chapterId}/audio")
    public ResponseEntity<ApiResponse<Void>> uploadAudio(
            @PathVariable Long bookId, @PathVariable Long chapterId,
            @Validated @RequestBody ChapterUploadAudioRequest request) {
        chapterService.uploadAudio(bookId, chapterId, request);
        return ResponseEntity.ok(ApiResponse.<Void>builder().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<ChapterResponse>> getChapters(@PathVariable Long bookId) {
        return ResponseEntity.ok(chapterService.getChaptersByBookId(bookId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<ChapterResponse>> createChapter(
            @PathVariable Long bookId,
            @Validated @RequestBody ChapterCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.<ChapterResponse>builder()
                .data(chapterService.createChapter(bookId, req))
                .build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{chapterId}/audio")
    public ResponseEntity<ApiResponse<?>> generateAudio(
            @PathVariable Long bookId, @PathVariable Long chapterId,
            @Validated @RequestBody ChapterGenerateAudioRequest req) {
        if (!bookId.equals(req.getBookId()) || !chapterId.equals(req.getChapterId())) {
            throw new BusinessException(
                    BusinessError.VALIDATION_FAILED);
        }
        chapterService.generateAudio(req);
        return ResponseEntity.ok(ApiResponse.builder().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{chapterId}")
    public ResponseEntity<ApiResponse<?>> update (
            @PathVariable Long bookId,
            @PathVariable Long chapterId,
            @Validated @RequestBody ChapterUpdateRequest req) {
        chapterService.update(bookId, chapterId, req);
        return ResponseEntity.ok(ApiResponse.builder().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{chapterId}")
    public ResponseEntity<Void> deleteChapter(
            @PathVariable Long chapterId) {
        chapterService.deleteChapter(chapterId);
        return ResponseEntity.noContent().build();
    }

}
