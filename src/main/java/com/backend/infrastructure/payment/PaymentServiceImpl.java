package com.backend.infrastructure.payment;

import com.backend.application.service.CustomerBookService;
import com.backend.domain.adapter.payment.PaymentService;
import com.backend.domain.adapter.repository.BookRepository;
import com.backend.domain.adapter.repository.PaymentRepository;
import com.backend.domain.adapter.storage.StorageService;
import com.backend.domain.dto.response.payment.PaymentResponse;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.mapper.PaymentMapper;
import com.backend.domain.model.Book;
import com.backend.domain.model.Payment;
import com.backend.domain.valueobject.BusinessError;
import com.backend.domain.valueobject.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final BookRepository bookRepository;
    private final CustomerBookService customerBookService;
    private final PaymentRepository paymentRepository;
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

        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));

        if (customerBookService.hasPurchasedBook(customerId, bookId)) {
            throw new BusinessException(BusinessError.BOOK_ALREADY_OWNED);
        }

        // BƯỚC 1: Tìm xem khách hàng này đã có đơn nào PENDING với cuốn sách này chưa
        Optional<Payment> existingPendingOpt = paymentRepository
            .findFirstPendingPayment(customerId, bookId);

        // BƯỚC 2: NẾU ĐÃ CÓ -> TÁI SỬ DỤNG NGAY, KHÔNG GHI THÊM DB
        if (existingPendingOpt.isPresent()) {
            Payment existing = existingPendingOpt.get();
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
        Payment payment = Payment.builder()
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

        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new BusinessException(BusinessError.PAYMENT_NOT_FOUND));

        if (!payment.getCustomerId().equals(customerId)) {
            throw new BusinessException(BusinessError.FORBIDDEN);
        }

        Book book = bookRepository.findById(payment.getBookId())
            .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));
        return buildResponse(payment, book);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPendingPaymentByBook(Long customerId, Long bookId) {
        if (customerId == null) {
            throw new BusinessException(BusinessError.UNAUTHORIZED);
        }
        if (bookId == null) {
            throw new BusinessException(BusinessError.BOOK_NOT_FOUND);
        }

        Optional<Payment> pendingOpt = paymentRepository
            .findFirstPendingPayment(customerId, bookId);

        if (pendingOpt.isEmpty()) {
            return null;
        }

        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));
        return buildResponse(pendingOpt.get(), book);
    }

    private PaymentResponse buildResponse(Payment payment, Book book) {
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
