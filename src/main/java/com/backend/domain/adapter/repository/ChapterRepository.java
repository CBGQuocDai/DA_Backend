package com.backend.domain.adapter.repository;

import com.backend.domain.model.Chapter;
import java.util.List;
import java.util.Optional;

public interface ChapterRepository {
    List<Chapter> findByBookId(Long bookId);
    Optional<Chapter> findById(Long id);
    Chapter save(Chapter chapter);
    Chapter update(Chapter chapter);
    List<Chapter> saveAll(List<Chapter> chapters);
    void delete(Long id);
}
