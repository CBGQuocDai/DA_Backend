package com.backend.application.service;


import com.backend.application.dto.request.book.chapter.ChapterCreateRequest;
import com.backend.application.dto.request.book.chapter.ChapterGenerateAudioRequest;
import com.backend.application.dto.request.book.chapter.ChapterUpdateRequest;
import com.backend.application.dto.request.book.chapter.GenerateAudioResult;
import com.backend.application.dto.response.chapter.ChapterResponse;
import java.util.List;

public interface ChapterService {
    List<ChapterResponse> getChaptersByBookId(Long bookId);
    void deleteChapter(Long chapterId);
    void generateAudio(ChapterGenerateAudioRequest req);
    void handleAudioResult(GenerateAudioResult result);

    ChapterResponse createChapter(Long bookId, ChapterCreateRequest req);
    void update(Long bookId, Long chapterId, ChapterUpdateRequest req);
}
