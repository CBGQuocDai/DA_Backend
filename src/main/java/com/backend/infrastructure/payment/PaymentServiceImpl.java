package com.backend.infrastructure.payment;

import com.backend.domain.adapter.payment.PaymentService;
import com.backend.domain.adapter.storage.StorageService;
import com.backend.domain.dto.response.payment.PaymentResponse;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.mapper.PaymentMapper;
import com.backend.domain.valueobject.BusinessError;
import com.backend.domain.valueobject.PaymentStatus;
import com.backend.infrastructure.persistence.entity.JpaBookEntity;
import com.backend.infrastructure.persistence.entity.JpaPaymentEntity;
import com.backend.infrastructure.persistence.repository.JpaBookRepository;
import com.backend.infrastructure.persistence.repository.JpaCustomerBookRepository;
import com.backend.infrastructure.persistence.repository.JpaPaymentRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final JpaBookRepository bookRepository;
    private final JpaCustomerBookRepository customerBookRepository;
    private final JpaPaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final StorageService storageService;

    private static final Random RANDOM = new SecureRandom();

    @Override
    @Transactional
    public PaymentResponse createPayment(Long customerId, Long bookId) {
        if (customerId == null) {
            throw new BusinessException(BusinessError.UNAUTHORIZED);
        }
        if (bookId == null) {
            throw new BusinessException(BusinessError.BOOK_NOT_FOUND);
        }

        JpaBookEntity book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));

        if (customerBookRepository.existsByCustomerIdAndBookId(customerId, bookId)) {
            throw new BusinessException(BusinessError.BOOK_ALREADY_OWNED);
        }

        // BƯỚC 1: Tìm xem khách hàng này đã có đơn nào PENDING với cuốn sách này chưa
        Optional<JpaPaymentEntity> existingPendingOpt = paymentRepository
            .findFirstByCustomerIdAndBookIdAndStatusOrderByCreatedAtDesc(customerId, bookId,
                PaymentStatus.PENDING);

        // BƯỚC 2: NẾU ĐÃ CÓ -> TÁI SỬ DỤNG NGAY, KHÔNG GHI THÊM DB
        if (existingPendingOpt.isPresent()) {
            JpaPaymentEntity existing = existingPendingOpt.get();
            log.info(
                "Tái sử dụng giao dịch PENDING đã tồn tại: PaymentCode={}, CustomerId={}, BookId={}",
                existing.getPaymentCode(), customerId, bookId);
            return buildResponse(existing, book); // Trả về đơn cũ kèm link QR cũ
        }

        // BƯỚC 3: NẾU CHƯA CÓ -> MỚI TẠO BẢN GHI MỚI
        BigDecimal amount =
            book.getPrice() != null && book.getPrice().compareTo(BigDecimal.ZERO) > 0
                ? book.getPrice()
                : BigDecimal.valueOf(100000);

        String paymentCode = generatePaymentCode();
        JpaPaymentEntity payment = JpaPaymentEntity.builder()
            .customerId(customerId)
            .bookId(bookId)
            .paymentCode(paymentCode)
            .amount(amount)
            .status(PaymentStatus.PENDING)
            .createdAt(Instant.now())
            .build();

        payment = paymentRepository.save(payment);
        log.info(
            "Tạo mới giao dịch thanh toán PENDING: PaymentCode={}, CustomerId={}, BookId={}, Amount={}",
            paymentCode, customerId, bookId, amount);

        return buildResponse(payment, book);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentStatus(Long paymentId, Long customerId) {
        if (customerId == null) {
            throw new BusinessException(BusinessError.UNAUTHORIZED);
        }

        JpaPaymentEntity payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new BusinessException(BusinessError.PAYMENT_NOT_FOUND));

        if (!payment.getCustomerId().equals(customerId)) {
            throw new BusinessException(BusinessError.FORBIDDEN);
        }

        JpaBookEntity book = bookRepository.findById(payment.getBookId()).orElse(null);
        return buildResponse(payment, book);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPendingPaymentByBook(Long customerId, Long bookId) {
        if (customerId == null) {
            throw new BusinessException(BusinessError.UNAUTHORIZED);
        }
        if (bookId == null) {
            return null;
        }

        Optional<JpaPaymentEntity> pendingOpt = paymentRepository
            .findFirstByCustomerIdAndBookIdAndStatusOrderByCreatedAtDesc(customerId, bookId,
                PaymentStatus.PENDING);

        if (pendingOpt.isEmpty()) {
            return null;
        }

        JpaBookEntity book = bookRepository.findById(bookId).orElse(null);
        return buildResponse(pendingOpt.get(), book);
    }

    private PaymentResponse buildResponse(JpaPaymentEntity payment, JpaBookEntity book) {
        PaymentResponse response = paymentMapper.toResponse(payment, book);

        // Sinh link QR VietQR đúng chuẩn SePay
        String qrUrl = String.format(
            "https://vietqr.app/img?bank=Vietcombank&acc=SBSEPAYL39V6TXCOZQD&template=compact&amount=%s&des=%s",
            payment.getAmount().toPlainString(),
            URLEncoder.encode(payment.getPaymentCode(), StandardCharsets.UTF_8)
        );
        response.setQrUrl(qrUrl);

        if (response.getBookCover() != null && !response.getBookCover().isBlank()) {
            response.setBookCover(storageService.createSignUrl(response.getBookCover(), 60L));
        }

        return response;
    }

    private String generatePaymentCode() {
        return "SP" + (System.currentTimeMillis() % 10000000L) + String.format("%03d",
            RANDOM.nextInt(1000));
    }
}
