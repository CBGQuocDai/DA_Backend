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
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaBookRepository extends JpaRepository<JpaBookEntity, Long> {

    /**
     * Khung query cơ bản cho thẻ sách gợi ý: chỉ lấy sách đã xuất bản (PUBLISHED),
     * tính điểm đánh giá trung bình và số lượt đánh giá, không nạp nội dung nặng.
     */
    String RECOMMENDATION_BOOKS = """
        SELECT b.id AS id, b.title AS title, b.price AS price,
               b.coverImage AS coverImage, b.status AS status,
               (SELECT COALESCE(AVG(r.rate), 0.0) FROM JpaReviewEntity r WHERE r.bookId = b.id) AS averageRating,
               (SELECT COUNT(r.id) FROM JpaReviewEntity r WHERE r.bookId = b.id) AS ratingCount
        FROM JpaBookEntity b
        WHERE b.status = com.backend.domain.valueobject.BookStatus.PUBLISHED
        """;

    // Điều kiện loại trừ các cuốn sách khách hàng đã mua hoặc đã thả tim yêu thích
    String EXCLUDE_CUSTOMER_BOOKS = """
          AND NOT EXISTS (
              SELECT 1 FROM JpaCustomerBookEntity owned
              WHERE owned.bookId = b.id AND owned.customerId = :customerId
          )
          AND NOT EXISTS (
              SELECT 1 FROM JpaBookFavoriteEntity favorite
              WHERE favorite.bookId = b.id AND favorite.customerId = :customerId
          )
        """;

    // Sắp xếp ưu tiên sách mới nhất
    String NEWEST_FIRST = " ORDER BY b.id DESC";

    // Lấy danh sách sách mới nhất 
    @Query(RECOMMENDATION_BOOKS + NEWEST_FIRST)
    List<BookListProjection> findNewestRecommendations(Pageable pageable);

    // Lấy sách gợi ý theo thể loại khách quan tâm (đã mua hoặc đã thích)
    @Query(RECOMMENDATION_BOOKS + EXCLUDE_CUSTOMER_BOOKS + """
          AND EXISTS (
              SELECT 1 FROM JpaBookCategoryEntity candidate
              JOIN JpaBookCategoryEntity preferred ON preferred.categoryId = candidate.categoryId
              WHERE candidate.bookId = b.id AND (
                  preferred.bookId IN (
                      SELECT owned.bookId FROM JpaCustomerBookEntity owned WHERE owned.customerId = :customerId
                  ) OR preferred.bookId IN (
                      SELECT favorite.bookId FROM JpaBookFavoriteEntity favorite WHERE favorite.customerId = :customerId
                  )
              )
          )
        """ + NEWEST_FIRST)
    List<BookListProjection> findCategoryRecommendations(@Param("customerId") Long customerId,
        Pageable pageable);

    // Lấy sách mới nhất cho khách (loại trừ sách đã mua và đã thích)
    @Query(RECOMMENDATION_BOOKS + EXCLUDE_CUSTOMER_BOOKS + NEWEST_FIRST)
    List<BookListProjection> findNewestRecommendationsForCustomer(
        @Param("customerId") Long customerId,
        Pageable pageable);

    // Lấy sách mới nhất cho khách, loại trừ thêm danh sách ID sách đã chọn trước đó
    @Query(RECOMMENDATION_BOOKS + EXCLUDE_CUSTOMER_BOOKS + " AND b.id NOT IN :selectedBookIds"
        + NEWEST_FIRST)
    List<BookListProjection> findNewestRecommendationsForCustomerExcluding(
        @Param("customerId") Long customerId,
        @Param("selectedBookIds") Collection<Long> selectedBookIds,
        Pageable pageable);

    // Lấy sách tương tự cùng thể loại hoặc cùng tác giả (loại trừ chính cuốn sách đang xem)
    @Query(RECOMMENDATION_BOOKS + """
          AND b.id <> :bookId AND (
              EXISTS (
                  SELECT 1 FROM JpaBookCategoryEntity candidate
                  JOIN JpaBookCategoryEntity source ON source.categoryId = candidate.categoryId
                  WHERE candidate.bookId = b.id AND source.bookId = :bookId
              ) OR EXISTS (
                  SELECT 1 FROM JpaBookAuthorEntity candidate
                  JOIN JpaBookAuthorEntity source ON source.authorId = candidate.authorId
                  WHERE candidate.bookId = b.id AND source.bookId = :bookId
              )
          )
        """ + NEWEST_FIRST)
    List<BookListProjection> findSimilarRecommendations(@Param("bookId") Long bookId,
        Pageable pageable);

    // Truy vấn nhanh trạng thái sách theo ID
    @Query("SELECT b.status FROM JpaBookEntity b WHERE b.id = :id")
    Optional<BookStatus> findStatusById(@Param("id") Long id);

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
    List<BookSalesProjection> findBestsellers(@Param("since") Instant since,
        @Param("from") Instant from,
        @Param("to") Instant to, Pageable pageable);

    @Modifying
    @Query("UPDATE JpaBookEntity b SET " +
            "b.title = :title, " +
            "b.price = :price, " +
            "b.description = :description, " +
            "b.coverImage = :coverImage, " +
            "b.contentFile = :contentFile, " +
            "b.voiceId = :voiceId, " +
            "b.status = :status " +
            "WHERE b.id = :id")
    void updateBook(
            @Param("id") Long id,
            @Param("title") String title,
            @Param("price") BigDecimal price,
            @Param("description") String description,
            @Param("coverImage") String coverImage,
            @Param("contentFile") String contentFile,
            @Param("voiceId") Long voiceId,
            @Param("status") BookStatus status
    );
}
