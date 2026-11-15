package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaCustomerBookEntity;
import com.backend.infrastructure.persistence.projection.PeriodCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface JpaCustomerBookRepository extends JpaRepository<JpaCustomerBookEntity, Long> {

    List<JpaCustomerBookEntity> findByCustomerId(Long customerId);

    void deleteByBookId(Long bookId);

    boolean existsByCustomerIdAndBookId(Long customerId, Long bookId);

    // Cập nhật trạng thái hoàn thành nghe cuốn sách của khách hàng
    @Modifying
    @Query("UPDATE JpaCustomerBookEntity cb SET cb.isReadComplete = :complete "
        + "WHERE cb.customerId = :customerId AND cb.bookId = :bookId")
    void updateReadComplete(@Param("customerId") Long customerId, @Param("bookId") Long bookId,
        @Param("complete") boolean complete);

    /**
     * Đếm lượt sở hữu trong kỳ [from, to) và kỳ trước [since, from) bằng một lượt quét [since,
     * to).
     */
    @Query("""
        SELECT COALESCE(SUM(CASE WHEN cb.purchaseAt >= :from THEN 1 ELSE 0 END), 0) AS purchases,
               COALESCE(SUM(CASE WHEN cb.purchaseAt < :from THEN 1 ELSE 0 END), 0) AS previousPurchases
        FROM JpaCustomerBookEntity cb
        WHERE cb.purchaseAt >= :since AND cb.purchaseAt < :to
        """)
    PeriodCountProjection countPurchases(@Param("since") Instant since, @Param("from") Instant from,
        @Param("to") Instant to);

    @Query("SELECT COUNT(DISTINCT cb.customerId) FROM JpaCustomerBookEntity cb")
    long countDistinctCustomers();
}
