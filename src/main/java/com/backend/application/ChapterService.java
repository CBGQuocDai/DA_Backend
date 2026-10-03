package com.backend.application;


import com.backend.domain.model.Chapter;
import java.util.List;

public interface ChapterService {
    List<Chapter> getChaptersByBookId(Long bookId);
    void deleteChapter(Long chapterId);
    void generateAudio(Long bookId, Long chapterId);

}
