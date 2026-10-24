package com.backend.application.service;

import com.backend.domain.dto.request.payment.SepayWebhookRequest;

public interface PaymentWebhookService {

    void processSepayWebhook(SepayWebhookRequest payload);
}
