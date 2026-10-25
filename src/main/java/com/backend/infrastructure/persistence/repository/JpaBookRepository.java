package com.backend.infrastructure.persistence.repository;

import com.backend.domain.valueobject.BookStatus;
import com.backend.infrastructure.persistence.entity.JpaBookEntity;
import com.backend.infrastructure.persistence.projection.BookListProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaBookRepository extends JpaRepository<JpaBookEntity, Long> {

    @Query("""
            SELECT b FROM JpaBookEntity b
            LEFT JOIN FETCH b.voice
            WHERE b.id = :id
            """)
    Optional<JpaBookEntity> findByIdWithVoice(@Param("id") Long id);

    @Query("""
            SELECT
                b.id          AS id,
                b.title       AS title,
                b.price       AS price,
                b.description AS description,
                b.coverImage  AS coverImage,
                b.contentFile AS contentFile,
                b.voiceId     AS voiceId,
                v.name        AS voiceName,
                v.exampleAudio AS voiceExampleAudio,
                v.description AS voiceDescription,
                b.status      AS status,
                COALESCE(AVG(r.rate), 0.0) AS averageRating
            FROM JpaBookEntity b
            LEFT JOIN JpaVoiceEntity v ON v.id = b.voiceId
            LEFT JOIN JpaReviewEntity r ON r.bookId = b.id
            WHERE LOWER(b.title) LIKE :bookName ESCAPE '\\'
              AND (:status IS NULL OR b.status = :status)
              AND (:categoryId IS NULL OR EXISTS (
                    SELECT 1 FROM JpaBookCategoryEntity bc
                    WHERE bc.bookId = b.id AND bc.categoryId = :categoryId))
              AND (:authorId IS NULL OR EXISTS (
                    SELECT 1 FROM JpaBookAuthorEntity ba
                    WHERE ba.bookId = b.id AND ba.authorId = :authorId))
            GROUP BY b.id, v.id
            """)
    Page<BookListProjection> findAllBookListProjections(@Param("bookName") String bookName,
                                                        @Param("categoryId") Long categoryId,
                                                        @Param("authorId") Long authorId,
                                                        @Param("status") BookStatus status,
                                                        Pageable pageable);
}
