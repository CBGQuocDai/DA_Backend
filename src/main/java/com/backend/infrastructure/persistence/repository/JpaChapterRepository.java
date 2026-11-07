package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaChapterEntity;
import com.backend.infrastructure.persistence.projection.CustomerChapterProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaChapterRepository extends JpaRepository<JpaChapterEntity, Long> {
    List<JpaChapterEntity> findByBookIdOrderByChapterOrderAsc(Long bookId);

    @Query(value = """
            SELECT c.id AS id, c.title AS title, c.chapterOrder AS chapterOrder,
                   c.duration AS duration, c.audioUrl AS audioUrl, c.status AS status
            FROM JpaChapterEntity c
            WHERE c.bookId = :bookId
            ORDER BY c.chapterOrder ASC, c.id ASC
            """, countQuery = "SELECT COUNT(c) FROM JpaChapterEntity c WHERE c.bookId = :bookId")
    Page<CustomerChapterProjection> findCustomerPageByBookId(@Param("bookId") Long bookId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(c.duration), 0) FROM JpaChapterEntity c WHERE c.bookId = :bookId")
    long sumDurationByBookId(@Param("bookId") Long bookId);

    @Query("""
            SELECT c FROM JpaChapterEntity c
            LEFT JOIN FETCH c.attachFile
            WHERE c.bookId = :bookId
            ORDER BY c.chapterOrder ASC
            """)
    List<JpaChapterEntity> findWithAttachFileByBookId(@Param("bookId") Long bookId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM JpaChapterEntity c WHERE c.id = :id")
    Optional<JpaChapterEntity> findByIdForUpdate(@Param("id") Long id);

    Optional<JpaChapterEntity> findByIdAndBookId(Long id, Long bookId);

    @Query("SELECT MAX(c.chapterOrder) FROM JpaChapterEntity c WHERE c.bookId = :bookId")
    Optional<Integer> findMaxChapterOrder(@Param("bookId") Long bookId);

    boolean existsByBookIdAndChapterOrder(Long bookId, Integer chapterOrder);

    void deleteByBookId(Long bookId);
}
