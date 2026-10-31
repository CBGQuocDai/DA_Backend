package com.backend.application.service;

import com.backend.application.dto.response.payment.PaymentResponse;

import java.math.BigDecimal;

public interface PaymentService {
    PaymentResponse createPayment(Long customerId, Long bookId);
    PaymentResponse getPaymentStatus(Long paymentId, Long customerId);
    PaymentResponse getPendingPaymentByBook(Long customerId, Long bookId);
    /** The money for {@code paymentCode} arrived: marks it paid and unlocks the book. Safe to call twice. */
    void confirmPayment(String paymentCode, BigDecimal paidAmount);
}
