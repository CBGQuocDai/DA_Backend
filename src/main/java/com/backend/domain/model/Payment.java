package com.backend.domain.model;

import com.backend.domain.valueobject.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    private Long id;
    private String paymentCode;
    private Long customerId;
    private Long bookId;
    private BigDecimal amount;
    private PaymentStatus status;
    private Instant paidAt;
    private Instant createdAt;
}
