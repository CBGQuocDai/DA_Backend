package com.backend.application.adapter.repository;

import com.backend.domain.model.Payment;
import com.backend.domain.valueobject.MonthlyTotal;
import com.backend.domain.valueobject.PaymentStatus;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    Optional<Payment> findById(Long id);
    Optional<Payment> findByPaymentCode(String paymentCode);
    Optional<Payment> findLatestByCustomerIdAndBookIdAndStatus(Long customerId, Long bookId, PaymentStatus status);
    Payment save(Payment payment);

    /** Paid payments per calendar month in {@code zone}, from {@code from} on; months without any are left out. */
    List<MonthlyTotal> sumPaidByMonth(Instant from, ZoneId zone);
}
