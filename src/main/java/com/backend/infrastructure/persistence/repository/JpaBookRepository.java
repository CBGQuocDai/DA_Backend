package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaBookEntity;
import com.backend.infrastructure.persistence.projection.BookListProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaBookRepository extends JpaRepository<JpaBookEntity, Long> {

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
            GROUP BY b.id, v.id
            """)
    Page<BookListProjection> findAllBookListProjections(Pageable pageable);
}
