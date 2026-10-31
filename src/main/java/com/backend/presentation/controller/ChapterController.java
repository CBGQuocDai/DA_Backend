package com.backend.presentation.controller;

import com.backend.application.service.ChapterService;
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

    @GetMapping
    public ResponseEntity<List<ChapterResponse>> getChapters(@PathVariable Long bookId) {
        return ResponseEntity.ok(chapterService.getChaptersByBookId(bookId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{chapterId}")
    public ResponseEntity<Void> deleteChapter(
            @PathVariable Long chapterId) {
        chapterService.deleteChapter(chapterId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{chapterId}/audio")
    public ResponseEntity<ApiResponse<?>> generateAudio(
            @Validated @RequestBody ChapterGenerateAudioRequest req) {
        chapterService.generateAudio(req);
        return ResponseEntity.ok(ApiResponse.builder().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{chapterId}")
    public ResponseEntity<ApiResponse<?>> update (
            @PathVariable Long chapterId,
            @RequestBody ChapterUpdateRequest req) {
        chapterService.update(chapterId, req);
        return ResponseEntity.ok(ApiResponse.builder().build());
    }
}
