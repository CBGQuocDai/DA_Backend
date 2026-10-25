package com.backend.application.service;


import com.backend.domain.dto.request.book.chapter.ChapterUpdateRequest;
import com.backend.domain.model.Chapter;
import java.util.List;

public interface ChapterService {
    List<Chapter> getChaptersByBookId(Long bookId);
    void deleteChapter(Long chapterId);
    void generateAudio(Long bookId, Long chapterId);

    void update(Long ChapterId, ChapterUpdateRequest req);
}
