package com.backend.infrastructure.persistence.repository;

import com.backend.domain.valueobject.BookStatus;
import com.backend.infrastructure.persistence.entity.JpaBookEntity;
import com.backend.infrastructure.persistence.projection.BookListProjection;
import com.backend.infrastructure.persistence.projection.BookSalesProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaBookRepository extends JpaRepository<JpaBookEntity, Long> {

    @Query("""
            SELECT b FROM JpaBookEntity b
            LEFT JOIN FETCH b.voice
            WHERE b.id = :id
            """)
    Optional<JpaBookEntity> findByIdWithVoice(@Param("id") Long id);

    // Không dùng fetch join vì PostgreSQL không cho khóa FOR UPDATE ở phía có thể null của outer join.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM JpaBookEntity b WHERE b.id = :id")
    Optional<JpaBookEntity> findBookOverviewById(@Param("id") Long id);

    @Query(value = """
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
            JOIN JpaBookCategoryEntity bc ON bc.bookId = b.id
            LEFT JOIN JpaVoiceEntity v ON v.id = b.voiceId
            LEFT JOIN JpaReviewEntity r ON r.bookId = b.id
            WHERE bc.categoryId = :categoryId
            GROUP BY b.id, v.id
            """,
            countQuery = """
            SELECT COUNT(bc.id)
            FROM JpaBookCategoryEntity bc
            WHERE bc.categoryId = :categoryId
            """)
    Page<BookListProjection> findBookListProjectionsByCategoryId(
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );
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


    /**
     * Lấy sách được mua trong [from, to), lượt mua giảm dần, kèm lượt mua kỳ trước [since, from);
     * quét một lần trong [since, to) và chia hai kỳ tại {@code from}.
     */
    @Query("""
            SELECT b.id AS id, b.title AS title, b.price AS price, b.coverImage AS coverImage,
                   SUM(CASE WHEN cb.purchaseAt >= :from THEN 1 ELSE 0 END) AS purchases,
                   SUM(CASE WHEN cb.purchaseAt < :from THEN 1 ELSE 0 END) AS previousPurchases
            FROM JpaCustomerBookEntity cb JOIN cb.book b
            WHERE cb.purchaseAt >= :since AND cb.purchaseAt < :to
            GROUP BY b.id, b.title, b.price, b.coverImage
            HAVING SUM(CASE WHEN cb.purchaseAt >= :from THEN 1 ELSE 0 END) > 0
            ORDER BY SUM(CASE WHEN cb.purchaseAt >= :from THEN 1 ELSE 0 END) DESC, b.id DESC
            """)
    List<BookSalesProjection> findBestsellers(@Param("since") Instant since, @Param("from") Instant from,
                                              @Param("to") Instant to, Pageable pageable);

}
