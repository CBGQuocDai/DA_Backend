package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaChapterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaChapterRepository extends JpaRepository<JpaChapterEntity, Long> {
    List<JpaChapterEntity> findByBookIdOrderByChapterOrderAsc(Long bookId);

    @Query("""
            SELECT c FROM JpaChapterEntity c
            LEFT JOIN FETCH c.attachFile
            WHERE c.bookId = :bookId
            ORDER BY c.chapterOrder ASC
            """)
    List<JpaChapterEntity> findWithAttachFileByBookId(@Param("bookId") Long bookId);

    void deleteByBookId(Long bookId);
}
