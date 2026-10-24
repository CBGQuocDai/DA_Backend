package com.backend.application.service;

import com.backend.domain.dto.response.payment.PaymentResponse;

public interface PaymentService {

    PaymentResponse createPayment(Long customerId, Long bookId);

    PaymentResponse getPaymentStatus(Long paymentId, Long customerId);

    PaymentResponse getPendingPaymentByBook(Long customerId, Long bookId);
}
