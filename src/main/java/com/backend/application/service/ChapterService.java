package com.backend.application.service;


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

    void update(Long ChapterId, ChapterUpdateRequest req);
}
