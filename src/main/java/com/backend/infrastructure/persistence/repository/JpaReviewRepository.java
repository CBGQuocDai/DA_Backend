package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaReviewEntity;
import com.backend.infrastructure.persistence.projection.ReviewListProjection;
import com.backend.infrastructure.persistence.projection.ReviewSummaryProjection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaReviewRepository extends JpaRepository<JpaReviewEntity, Long> {

    String REVIEW_VIEW = """
            SELECT r.id AS id, r.bookId AS bookId, r.customerId AS userId,
                   c.fullName AS userName, c.avatarUrl AS userAvatar,
                   r.rate AS rating, r.content AS comment, r.createAt AS createdAt,
                   CASE WHEN EXISTS (SELECT cb.id FROM JpaCustomerBookEntity cb
                        WHERE cb.bookId = r.bookId AND cb.customerId = r.customerId)
                        THEN TRUE ELSE FALSE END AS verifiedPurchase
            FROM JpaReviewEntity r LEFT JOIN r.customer c
            """;

    @Query(value = REVIEW_VIEW + " WHERE r.bookId = :bookId AND (:rating IS NULL OR r.rate = :rating)",
            countQuery = "SELECT COUNT(r) FROM JpaReviewEntity r WHERE r.bookId = :bookId AND (:rating IS NULL OR r.rate = :rating)")
    Page<ReviewListProjection> findReviewPage(@Param("bookId") Long bookId,
                                             @Param("rating") Double rating, Pageable pageable);

    @Query(REVIEW_VIEW + " WHERE r.bookId = :bookId AND r.customerId = :customerId")
    List<ReviewListProjection> findOwnReview(@Param("bookId") Long bookId,
                                            @Param("customerId") Long customerId, Pageable pageable);

    @Query("""
            SELECT COUNT(r) AS totalReviews, AVG(r.rate) AS averageRating,
                   SUM(CASE WHEN r.rate = 1 THEN 1 ELSE 0 END) AS oneStar,
                   SUM(CASE WHEN r.rate = 2 THEN 1 ELSE 0 END) AS twoStars,
                   SUM(CASE WHEN r.rate = 3 THEN 1 ELSE 0 END) AS threeStars,
                   SUM(CASE WHEN r.rate = 4 THEN 1 ELSE 0 END) AS fourStars,
                   SUM(CASE WHEN r.rate = 5 THEN 1 ELSE 0 END) AS fiveStars
            FROM JpaReviewEntity r WHERE r.bookId = :bookId
            """)
    ReviewSummaryProjection summarizeByBookId(@Param("bookId") Long bookId);

    @Query("""
            SELECT COUNT(r) AS totalReviews, AVG(r.rate) AS averageRating,
                   SUM(CASE WHEN r.rate = 1 THEN 1 ELSE 0 END) AS oneStar,
                   SUM(CASE WHEN r.rate = 2 THEN 1 ELSE 0 END) AS twoStars,
                   SUM(CASE WHEN r.rate = 3 THEN 1 ELSE 0 END) AS threeStars,
                   SUM(CASE WHEN r.rate = 4 THEN 1 ELSE 0 END) AS fourStars,
                   SUM(CASE WHEN r.rate = 5 THEN 1 ELSE 0 END) AS fiveStars
            FROM JpaReviewEntity r
            """)
    ReviewSummaryProjection summarizeAll();

    boolean existsByBookIdAndCustomerId(Long bookId, Long customerId);

    void deleteByBookId(Long bookId);
}
