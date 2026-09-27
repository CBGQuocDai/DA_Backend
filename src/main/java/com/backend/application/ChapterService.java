package com.backend.application;

import com.backend.domain.dto.request.book.ChapterMergeRequest;
import com.backend.domain.dto.request.book.ChapterSplitRequest;
import com.backend.domain.dto.request.book.ChapterUpdateRequest;
import com.backend.domain.model.Chapter;
import java.util.List;

public interface ChapterService {
    List<Chapter> getChaptersByBookId(Long bookId);
    Chapter getChapter(Long bookId, Long chapterId);
    Chapter updateChapter(Long bookId, Long chapterId, ChapterUpdateRequest request);
    Chapter splitChapter(Long bookId, Long chapterId, ChapterSplitRequest request);
    List<Chapter> mergeChapters(Long bookId, ChapterMergeRequest request);
    void deleteChapter(Long bookId, Long chapterId);
}
