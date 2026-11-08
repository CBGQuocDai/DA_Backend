package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaCustomerBookEntity;
import com.backend.infrastructure.persistence.projection.PeriodCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface JpaCustomerBookRepository extends JpaRepository<JpaCustomerBookEntity, Long> {
    List<JpaCustomerBookEntity> findByCustomerId(Long customerId);
    void deleteByBookId(Long bookId);
    boolean existsByCustomerIdAndBookId(Long customerId, Long bookId);

    /** Purchases in [from, to) and in [since, from), in one pass over [since, to). */
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
