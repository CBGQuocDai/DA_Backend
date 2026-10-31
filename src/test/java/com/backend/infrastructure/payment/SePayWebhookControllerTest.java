package com.backend.infrastructure.payment;

import com.backend.application.service.PaymentService;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.valueobject.BusinessError;
import com.backend.presentation.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SePayWebhookControllerTest {

    private static final String SECRET = "test-secret";
    // SePay may add fields at any time: an unknown one must not reject the payment
    private static final String BODY = """
            {"id": 1, "code": "SP123", "transferAmount": 10, "fieldSePayAddedLater": "x"}
            """;

    @Mock
    private PaymentService paymentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SePayWebhookController controller = new SePayWebhookController(paymentService);
        ReflectionTestUtils.setField(controller, "sePaySecretKey", SECRET);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static String sign(String timestamp, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void handleWebhook_processesASignedPayloadEvenWithUnknownFields() throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        mockMvc.perform(post("/payments/sepay/webhook").contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .header("X-SePay-Signature", sign(timestamp, BODY))
                        .header("X-SePay-Timestamp", timestamp))
                .andExpect(status().isOk());

        verify(paymentService).confirmPayment("SP123", new BigDecimal("10"));
    }

    private void postSigned(String body, int expectedStatus) throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        mockMvc.perform(post("/payments/sepay/webhook").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("X-SePay-Signature", sign(timestamp, body))
                        .header("X-SePay-Timestamp", timestamp))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void handleWebhook_findsThePaymentCodeInTheTransferContent() throws Exception {
        postSigned("""
                {"content": "Thanh toan sp456 cam on", "transferAmount": 10}
                """, 200);

        verify(paymentService).confirmPayment("SP456", new BigDecimal("10"));
    }

    @Test
    void handleWebhook_fallsBackToTheDescription() throws Exception {
        postSigned("""
                {"content": "Khong co ma", "description": "MBVCB SP789", "transferAmount": 10}
                """, 200);

        verify(paymentService).confirmPayment("SP789", new BigDecimal("10"));
    }

    @Test
    void handleWebhook_rejectsATransferWithoutAPaymentCode() throws Exception {
        postSigned("""
                {"content": "Chuyen tien an trua", "transferAmount": 10}
                """, 404);

        verifyNoInteractions(paymentService);
    }

    @Test
    void handleWebhook_mapsBusinessErrorsInsteadOfBlamingThePayload() throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        doThrow(new BusinessException(BusinessError.PAYMENT_NOT_FOUND))
                .when(paymentService).confirmPayment(any(), any());

        mockMvc.perform(post("/payments/sepay/webhook").contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .header("X-SePay-Signature", sign(timestamp, BODY))
                        .header("X-SePay-Timestamp", timestamp))
                .andExpect(status().isNotFound());
    }

    @Test
    void handleWebhook_rejectsAMalformedPayload() throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String malformed = "{not json";

        mockMvc.perform(post("/payments/sepay/webhook").contentType(MediaType.APPLICATION_JSON).content(malformed)
                        .header("X-SePay-Signature", sign(timestamp, malformed))
                        .header("X-SePay-Timestamp", timestamp))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    void handleWebhook_rejectsAWrongSignature() throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        mockMvc.perform(post("/payments/sepay/webhook").contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .header("X-SePay-Signature", "sha256=sai")
                        .header("X-SePay-Timestamp", timestamp))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(paymentService);
    }
}
