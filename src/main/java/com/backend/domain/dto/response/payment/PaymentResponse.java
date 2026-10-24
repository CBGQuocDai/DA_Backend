package com.backend.domain.dto.response.payment;

import com.backend.domain.valueobject.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {

    private Long id;
    private Long paymentId;
    private String paymentCode;
    private String orderCode;
    private Long customerId;
    private Long bookId;
    private String bookTitle;
    private String bookCover;
    private BigDecimal amount;
    private PaymentStatus status;
    private PaymentStatus paymentStatus;

    private Instant paidAt;
    private Instant createdAt;

    private String qrUrl;
}
