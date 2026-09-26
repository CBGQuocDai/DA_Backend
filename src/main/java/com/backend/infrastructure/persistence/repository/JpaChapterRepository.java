package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaChapterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaChapterRepository extends JpaRepository<JpaChapterEntity, Long> {
    List<JpaChapterEntity> findByBookIdOrderByChapterOrderAsc(Long bookId);
}
