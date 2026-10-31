package com.backend.infrastructure.payment;

import com.backend.application.service.PaymentService;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.valueobject.BusinessError;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/payments/sepay")
@RequiredArgsConstructor
public class SePayWebhookController {

    private static final Pattern PAYMENT_CODE_PATTERN = Pattern.compile("(?i)SP\\d+");

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Value("${sepay.webhook.secret}")
    private String sePaySecretKey;

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-SePay-Signature", required = false) String signatureHeader,
            @RequestHeader(value = "X-SePay-Timestamp", required = false) String timestampHeader
    ) {
        log.info("Nhận SePay Webhook - Signature: {}, Timestamp: {}", signatureHeader, timestampHeader);

        // 1. Kiểm tra headers bắt buộc
        if (signatureHeader == null || signatureHeader.isBlank() || timestampHeader == null || timestampHeader.isBlank()) {
            log.warn("Từ chối: Thiếu Header X-SePay-Signature hoặc X-SePay-Timestamp");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Missing security headers"));
        }

        // 2. Chống Replay Attack: Kiểm tra thời gian timestamp (cho phép lệch tối đa 5 phút = 300s)
        try {
            long requestTimestamp = Long.parseLong(timestampHeader);
            long currentTimestamp = Instant.now().getEpochSecond();
            if (Math.abs(currentTimestamp - requestTimestamp) > 300) {
                log.warn("Từ chối: Timestamp quá cũ hoặc lệch giờ hệ thống (request: {}, server: {})", requestTimestamp, currentTimestamp);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Timestamp expired"));
            }
        } catch (NumberFormatException e) {
            log.warn("Từ chối: Timestamp không đúng định dạng số: {}", timestampHeader);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Invalid timestamp format"));
        }

        // 3. Tính toán chữ ký HMAC-SHA256: message = timestamp + "." + rawBody
        String expectedSignature = calculateHmacSha256(timestampHeader + "." + rawBody, sePaySecretKey);

        // 4. So sánh an toàn chống timing attack
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
                                  signatureHeader.getBytes(StandardCharsets.UTF_8))) {
            log.warn("Từ chối: Chữ ký HMAC SePay không hợp lệ! Kỳ vọng: {}, Nhận được: {}", expectedSignature, signatureHeader);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Invalid signature"));
        }

        // 5. Chữ ký hợp lệ -> parse JSON sang DTO và xử lý tiếp
        SePayWebhookRequest payload;
        try {
            payload = objectMapper.readValue(rawBody, SePayWebhookRequest.class);
            log.info("Xác thực HMAC thành công! Giao dịch ID: {}, Số tiền: {}, Nội dung: '{}', Gateway: {}",
                    payload.getId(),
                    payload.getTransferAmount(),
                    payload.getContent(),
                    payload.getGateway());
        } catch (Exception e) {
            log.error("Lỗi khi giải mã payload JSON của SePay", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Invalid payload format"));
        }

        // 6. Tách mã thanh toán khỏi format của SePay rồi chuyển cho nghiệp vụ
        String paymentCode = extractPaymentCode(payload);
        if (paymentCode == null) {
            log.error("Không tìm thấy mã thanh toán hợp lệ trong nội dung chuyển khoản: '{}'", payload.getContent());
            throw new BusinessException(BusinessError.PAYMENT_NOT_FOUND);
        }
        paymentService.confirmPayment(paymentCode, payload.getTransferAmount());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Webhook verified and processed successfully"
        ));
    }

    private String extractPaymentCode(SePayWebhookRequest payload) {
        if (payload.getCode() != null && !payload.getCode().isBlank()) {
            return payload.getCode().trim().toUpperCase();
        }
        if (payload.getContent() != null) {
            Matcher matcher = PAYMENT_CODE_PATTERN.matcher(payload.getContent());
            if (matcher.find()) {
                return matcher.group().toUpperCase();
            }
        }
        if (payload.getDescription() != null) {
            Matcher matcher = PAYMENT_CODE_PATTERN.matcher(payload.getDescription());
            if (matcher.find()) {
                return matcher.group().toUpperCase();
            }
        }
        return null;
    }

    private String calculateHmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tính HMAC-SHA256", e);
        }
    }
}
