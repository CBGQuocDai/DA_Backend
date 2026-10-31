package com.backend.application.adapter.repository;

import com.backend.domain.model.Book;
import com.backend.domain.model.Chapter;
import com.backend.domain.valueobject.ChapterStatus;
import java.util.List;
import java.util.Optional;

public interface ChapterRepository {
    List<Chapter> findByBookId(Long bookId);
    Optional<Chapter> findById(Long id);
    /** Loads the chapter with a pessimistic write lock; waits for any in-flight transaction on the row. */
    Optional<Chapter> findByIdForUpdate(Long id);
    Chapter save(Chapter chapter);
    Chapter update(Chapter chapter);
    List<Chapter> saveAll(List<Chapter> chapters, Book book);
    void delete(Long id);

    /** Updates only audio_url, duration and status. */
    void updateAudio(Long id, String audioUrl, Integer duration, ChapterStatus status);

    /** Atomically moves the chapter to PROCESSING_AUDIO; false if it already is, or does not exist. */
    boolean markProcessingAudio(Long id, Double speed);
}
