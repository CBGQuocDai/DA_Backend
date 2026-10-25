package com.backend.infrastructure.payment;

import com.backend.domain.adapter.payment.PaymentWebhookService;
import com.backend.domain.dto.request.payment.SepayWebhookRequest;
import com.backend.domain.valueobject.PaymentStatus;
import com.backend.infrastructure.persistence.entity.JpaCustomerBookEntity;
import com.backend.infrastructure.persistence.entity.JpaPaymentEntity;
import com.backend.infrastructure.persistence.repository.JpaCustomerBookRepository;
import com.backend.infrastructure.persistence.repository.JpaPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookServiceImpl implements PaymentWebhookService {

    private final JpaPaymentRepository paymentRepository;
    private final JpaCustomerBookRepository customerBookRepository;

    private static final Pattern PAYMENT_CODE_PATTERN = Pattern.compile("(?i)SP\\d+");

    @Override
    @Transactional
    public void processSepayWebhook(SepayWebhookRequest payload) {
        log.info("========== XỬ LÝ SEPAY WEBHOOK ==========");
        log.info("Giao dịch ID: {}", payload.getId());
        log.info("Ngân hàng: {}, Số tiền: {}, Nội dung: '{}'",
                payload.getGateway(), payload.getTransferAmount(), payload.getContent());

        // 1. Trích xuất mã thanh toán (paymentCode) từ content hoặc code
        String paymentCode = extractPaymentCode(payload);
        if (paymentCode == null) {
            log.warn("Không tìm thấy mã thanh toán hợp lệ trong nội dung chuyển khoản: '{}'", payload.getContent());
            return;
        }

        // 2. Tìm bản ghi trong tbl_payment
        Optional<JpaPaymentEntity> paymentOpt = paymentRepository.findByPaymentCode(paymentCode);
        if (paymentOpt.isEmpty()) {
            log.warn("Không tìm thấy giao dịch thanh toán trong hệ thống với mã: {}", paymentCode);
            return;
        }

        JpaPaymentEntity payment = paymentOpt.get();

        // 3. Nếu đã thanh toán rồi thì bỏ qua (Idempotency)
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("Giao dịch {} đã được xử lý thanh toán trước đó.", paymentCode);
            return;
        }

        // 4. Kiểm tra số tiền chuyển khoản
        if (payload.getTransferAmount() != null &&
                payload.getTransferAmount().compareTo(payment.getAmount()) < 0) {
            log.warn("Số tiền chuyển khoản ({} đ) nhỏ hơn số tiền đơn hàng ({} đ) cho mã {}",
                    payload.getTransferAmount(), payment.getAmount(), paymentCode);
            return;
        }

        // 5. Cập nhật trạng thái tbl_payment -> PAID
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(Instant.now());
        paymentRepository.save(payment);
        log.info("Đã cập nhật trạng thái thanh toán thành công cho mã {}", paymentCode);

        // 6. Mở khóa sách cho khách hàng (thêm vào tbl_customer_book nếu chưa có)
        boolean alreadyOwned = customerBookRepository.existsByCustomerIdAndBookId(
                payment.getCustomerId(), payment.getBookId());

        if (!alreadyOwned) {
            JpaCustomerBookEntity customerBook = new JpaCustomerBookEntity();
            customerBook.setCustomerId(payment.getCustomerId());
            customerBook.setBookId(payment.getBookId());
            customerBook.setPurchaseAt(Instant.now());
            customerBook.setIsReadComplete(false);
            customerBookRepository.save(customerBook);

            log.info("Đã mở khóa sách ID={} cho khách hàng ID={}",
                    payment.getBookId(), payment.getCustomerId());
        } else {
            log.info("Khách hàng ID={} đã sở hữu sách ID={}",
                    payment.getCustomerId(), payment.getBookId());
        }

        log.info("========== HOÀN TẤT XỬ LÝ GIAO DỊCH {} ==========", paymentCode);
    }

    private String extractPaymentCode(SepayWebhookRequest payload) {
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
}
