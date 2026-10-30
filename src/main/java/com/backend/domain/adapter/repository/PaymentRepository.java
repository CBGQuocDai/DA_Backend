package com.backend.domain.adapter.repository;

import com.backend.domain.model.Payment;

import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(Long id);
    Optional<Payment> findByPaymentCode(String paymentCode);
    Optional<Payment> findFirstPendingPayment(Long customerId, Long bookId);
}
