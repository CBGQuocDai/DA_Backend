package com.backend.domain.mapper;

import com.backend.domain.dto.response.payment.PaymentResponse;
import com.backend.domain.model.Book;
import com.backend.domain.model.Payment;
import com.backend.infrastructure.persistence.entity.JpaPaymentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    Payment toDomain(JpaPaymentEntity entity);

    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "book", ignore = true)
    JpaPaymentEntity toEntity(Payment domain);

    default PaymentResponse toResponse(Payment payment) {
        return toResponse(payment, null);
    }

    default PaymentResponse toResponse(Payment payment, Book book) {
        if (payment == null) {
            return null;
        }
        String bookTitle = book != null ? book.getTitle() : null;
        String bookCover = book != null ? book.getCoverImage() : null;

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
