package com.backend.presentation.controller;

import com.backend.application.ChapterService;
import com.backend.domain.dto.response.ApiResponse;
import com.backend.domain.model.Chapter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/books/{bookId}/chapters")
public class ChapterController {

    private final ChapterService chapterService;

    @GetMapping
    public ResponseEntity<List<Chapter>> getChapters(@PathVariable Long bookId) {
        return ResponseEntity.ok(chapterService.getChaptersByBookId(bookId));
    }


    @DeleteMapping("/{chapterId}")
    public ResponseEntity<Void> deleteChapter(
            @PathVariable Long chapterId) {
        chapterService.deleteChapter(chapterId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{chapterId}/audio")
    public ResponseEntity<ApiResponse<?>> generateAudio(
            @PathVariable Long bookId,
            @PathVariable Long chapterId) {
        chapterService.generateAudio(bookId, chapterId);
        return ResponseEntity.ok(ApiResponse.builder().build());
    }
}
