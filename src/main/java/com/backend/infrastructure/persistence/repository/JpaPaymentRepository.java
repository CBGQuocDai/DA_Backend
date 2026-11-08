package com.backend.infrastructure.persistence.repository;

import com.backend.domain.valueobject.PaymentStatus;
import com.backend.infrastructure.persistence.entity.JpaPaymentEntity;
import com.backend.infrastructure.persistence.projection.MonthlyTotalProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaPaymentRepository extends JpaRepository<JpaPaymentEntity, Long> {

    Optional<JpaPaymentEntity> findByPaymentCode(String paymentCode);

    Optional<JpaPaymentEntity> findFirstByCustomerIdAndBookIdAndStatusOrderByCreatedAtDesc(Long customerId, Long bookId, PaymentStatus status);

    /** Paid payments grouped by calendar month in {@code zone} (an IANA id such as Asia/Ho_Chi_Minh). */
    @Query(value = """
            SELECT t.yr AS yr, t.mo AS mo, COUNT(*) AS total, SUM(t.amount) AS amount
            FROM (SELECT CAST(EXTRACT(YEAR FROM p.paid_at AT TIME ZONE :zone) AS INTEGER) AS yr,
                         CAST(EXTRACT(MONTH FROM p.paid_at AT TIME ZONE :zone) AS INTEGER) AS mo,
                         p.amount AS amount
                  FROM tbl_payment p
                  WHERE p.status = 'PAID' AND p.paid_at >= :from) t
            GROUP BY t.yr, t.mo
            """, nativeQuery = true)
    List<MonthlyTotalProjection> sumPaidByMonth(@Param("from") Instant from, @Param("zone") String zone);
}
