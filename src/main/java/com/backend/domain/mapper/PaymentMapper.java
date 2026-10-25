package com.backend.domain.mapper;

import com.backend.domain.dto.response.payment.PaymentResponse;
import com.backend.infrastructure.persistence.entity.JpaBookEntity;
import com.backend.infrastructure.persistence.entity.JpaPaymentEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(JpaPaymentEntity payment) {
        return toResponse(payment, null);
    }

    public PaymentResponse toResponse(JpaPaymentEntity payment, JpaBookEntity book) {
        String bookTitle = book != null ? book.getTitle() : (payment.getBook() != null ? payment.getBook().getTitle() : null);
        String bookCover = book != null ? book.getCoverImage() : (payment.getBook() != null ? payment.getBook().getCoverImage() : null);

        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentId(payment.getId())
                .paymentCode(payment.getPaymentCode())
                .orderCode(payment.getPaymentCode())
                .customerId(payment.getCustomerId())
                .bookId(payment.getBookId())
                .bookTitle(bookTitle)
                .bookCover(bookCover)
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentStatus(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
