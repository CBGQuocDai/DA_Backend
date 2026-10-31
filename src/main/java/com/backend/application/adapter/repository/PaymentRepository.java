package com.backend.application.adapter.repository;

import com.backend.domain.model.Payment;
import com.backend.domain.valueobject.PaymentStatus;

import java.util.Optional;

public interface PaymentRepository {
    Optional<Payment> findById(Long id);
    Optional<Payment> findByPaymentCode(String paymentCode);
    Optional<Payment> findLatestByCustomerIdAndBookIdAndStatus(Long customerId, Long bookId, PaymentStatus status);
    Payment save(Payment payment);
}
