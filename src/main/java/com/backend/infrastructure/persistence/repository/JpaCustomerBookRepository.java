package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaCustomerBookEntity;
import com.backend.infrastructure.persistence.projection.PurchaseCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface JpaCustomerBookRepository extends JpaRepository<JpaCustomerBookEntity, Long> {
    List<JpaCustomerBookEntity> findByCustomerId(Long customerId);
    void deleteByBookId(Long bookId);
    boolean existsByCustomerIdAndBookId(Long customerId, Long bookId);

    @Query("""
            SELECT cb.bookId AS bookId, COUNT(cb) AS purchases FROM JpaCustomerBookEntity cb
            WHERE cb.bookId IN :bookIds AND cb.purchaseAt >= :from AND cb.purchaseAt < :to
            GROUP BY cb.bookId
            """)
    List<PurchaseCountProjection> countPurchasesByBook(@Param("bookIds") Collection<Long> bookIds,
                                                       @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(cb) FROM JpaCustomerBookEntity cb WHERE cb.purchaseAt >= :from AND cb.purchaseAt < :to")
    long countPurchasesBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(DISTINCT cb.customerId) FROM JpaCustomerBookEntity cb")
    long countDistinctCustomers();
}
