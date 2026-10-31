package com.backend.application.adapter.payment;

import com.backend.domain.model.Payment;

public interface PaymentGateway {

    /** Link of the QR code the customer scans to pay {@code payment}. */
    String createQrUrl(Payment payment);
}
