package com.backend.domain.adapter.repository;

import com.backend.domain.model.Book;
import com.backend.domain.model.Chapter;
import java.util.List;
import java.util.Optional;

public interface ChapterRepository {
    List<Chapter> findByBookId(Long bookId);
    Optional<Chapter> findById(Long id);
    Chapter save(Chapter chapter);
    Chapter update(Chapter chapter);
    List<Chapter> saveAll(List<Chapter> chapters, Book book);
    void delete(Long id);
}
