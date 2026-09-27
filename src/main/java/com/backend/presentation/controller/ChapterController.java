package com.backend.presentation.controller;

import com.backend.application.AudioService;
import com.backend.application.ChapterService;
import com.backend.domain.dto.response.AudioGenerationResponse;
import com.backend.domain.dto.request.book.ChapterMergeRequest;
import com.backend.domain.dto.request.book.ChapterSplitRequest;
import com.backend.domain.dto.request.book.ChapterUpdateRequest;
import com.backend.domain.model.Chapter;
import com.backend.domain.valueobject.ChapterStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/books/{bookId}/chapters")
public class ChapterController {

    private final ChapterService chapterService;
    private final AudioService audioService;

    @GetMapping
    public ResponseEntity<List<Chapter>> getChapters(@PathVariable Long bookId) {
        return ResponseEntity.ok(chapterService.getChaptersByBookId(bookId));
    }

    @GetMapping("/{chapterId}")
    public ResponseEntity<Chapter> getChapter(@PathVariable Long bookId, @PathVariable Long chapterId) {
        return ResponseEntity.ok(chapterService.getChapter(bookId, chapterId));
    }

    @PutMapping("/{chapterId}")
    public ResponseEntity<Chapter> updateChapter(
            @PathVariable Long bookId,
            @PathVariable Long chapterId,
            @RequestBody ChapterUpdateRequest request) {
        return ResponseEntity.ok(chapterService.updateChapter(bookId, chapterId, request));
    }

    @PostMapping("/{chapterId}/split")
    public ResponseEntity<Chapter> splitChapter(
            @PathVariable Long bookId,
            @PathVariable Long chapterId,
            @RequestBody ChapterSplitRequest request) {
        return ResponseEntity.ok(chapterService.splitChapter(bookId, chapterId, request));
    }

    @PostMapping("/merge")
    public ResponseEntity<List<Chapter>> mergeChapters(
            @PathVariable Long bookId,
            @RequestBody ChapterMergeRequest request) {
        return ResponseEntity.ok(chapterService.mergeChapters(bookId, request));
    }

    @DeleteMapping("/{chapterId}")
    public ResponseEntity<Void> deleteChapter(
            @PathVariable Long bookId,
            @PathVariable Long chapterId) {
        chapterService.deleteChapter(bookId, chapterId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{chapterId}/audio")
    public ResponseEntity<AudioGenerationResponse> generateAudio(
            @PathVariable Long bookId,
            @PathVariable Long chapterId) {
        return ResponseEntity.ok(audioService.generateAudio(bookId, chapterId));
    }

    @PostMapping("/{chapterId}/audio/upload")
    public ResponseEntity<AudioGenerationResponse> uploadAudio(
            @PathVariable Long bookId,
            @PathVariable Long chapterId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(audioService.uploadAudio(bookId, chapterId, file));
    }

    @GetMapping("/{chapterId}/audio/status")
    public ResponseEntity<ChapterStatus> getAudioStatus(@PathVariable Long chapterId) {
        return ResponseEntity.ok(audioService.getAudioStatus(chapterId));
    }
}
