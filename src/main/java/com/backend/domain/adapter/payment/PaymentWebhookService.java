package com.backend.domain.adapter.payment;

import com.backend.domain.dto.request.payment.SepayWebhookRequest;

public interface PaymentWebhookService {

    void processSepayWebhook(SepayWebhookRequest payload);
}
