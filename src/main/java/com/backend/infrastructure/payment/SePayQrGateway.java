package com.backend.infrastructure.payment;

import com.backend.application.adapter.payment.PaymentGateway;
import com.backend.domain.model.Payment;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class SePayQrGateway implements PaymentGateway {

    @Override
    public String createQrUrl(Payment payment) {
        // Sinh link QR VietQR đúng chuẩn SePay
        return String.format(
            "https://vietqr.app/img?bank=Vietcombank&acc=SBSEPAYL39V6TXCOZQD&template=compact&amount=%s&des=%s",
            payment.getAmount().toPlainString(),
            URLEncoder.encode(payment.getPaymentCode(), StandardCharsets.UTF_8)
        );
    }
}
