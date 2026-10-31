package com.backend.application.service.impl;

import org.springframework.transaction.annotation.Transactional;
import com.backend.application.dto.response.payment.PaymentResponse;
import com.backend.application.mapper.ResponseMapper;
import com.backend.application.adapter.payment.PaymentGateway;
import com.backend.application.adapter.repository.BookRepository;
import com.backend.application.adapter.repository.PaymentRepository;
import com.backend.application.service.CustomerBookService;
import com.backend.application.service.PaymentService;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Book;
import com.backend.domain.model.Payment;
import com.backend.domain.valueobject.BusinessError;
import com.backend.domain.valueobject.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final Random RANDOM = new SecureRandom();

    private final BookRepository bookRepository;
    private final CustomerBookService customerBookService;
    private final PaymentRepository paymentRepository;
    private final ResponseMapper responseMapper;
    private final PaymentGateway paymentGateway;

    @Override
    @Transactional
    public PaymentResponse createPayment(Long customerId, Long bookId) {
        if (customerId == null) {
            throw new BusinessException(BusinessError.UNAUTHORIZED);
        }
        if (bookId == null) {
            throw new BusinessException(BusinessError.BOOK_NOT_FOUND);
        }

        Book book = bookRepository.findBasicById(bookId)
            .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));

        if (customerBookService.hasPurchasedBook(customerId, bookId)) {
            throw new BusinessException(BusinessError.BOOK_ALREADY_OWNED);
        }

        // BƯỚC 1: Tìm xem khách hàng này đã có đơn nào PENDING với cuốn sách này chưa
        Optional<Payment> existingPendingOpt = paymentRepository
            .findLatestByCustomerIdAndBookIdAndStatus(customerId, bookId, PaymentStatus.PENDING);

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

        Payment payment = paymentRepository.save(Payment.builder()
            .customerId(customerId)
            .bookId(bookId)
            .paymentCode(paymentCode)
            .amount(amount)
            .status(PaymentStatus.PENDING)
            .createdAt(Instant.now())
            .build());
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

        Book book = bookRepository.findBasicById(payment.getBookId())
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
            .findLatestByCustomerIdAndBookIdAndStatus(customerId, bookId, PaymentStatus.PENDING);
        if (pendingOpt.isEmpty()) {
            return null;
        }

        Book book = bookRepository.findBasicById(bookId)
            .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));
        return buildResponse(pendingOpt.get(), book);
    }

    @Override
    @Transactional
    public void confirmPayment(String paymentCode, BigDecimal paidAmount) {
        // 1. Tìm bản ghi trong tbl_payment
        Payment payment = paymentRepository.findByPaymentCode(paymentCode)
            .orElseThrow(() -> {
                log.error("Không tìm thấy giao dịch thanh toán trong hệ thống với mã: {}", paymentCode);
                return new BusinessException(BusinessError.PAYMENT_NOT_FOUND);
            });

        // 2. Nếu đã thanh toán rồi thì bỏ qua (Idempotency)
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("Giao dịch {} đã được xử lý thanh toán trước đó.", paymentCode);
            return;
        }

        // 3. Kiểm tra số tiền chuyển khoản
        if (paidAmount != null && paidAmount.compareTo(payment.getAmount()) < 0) {
            log.error("Số tiền chuyển khoản ({} đ) nhỏ hơn số tiền đơn hàng ({} đ) cho mã {}",
                paidAmount, payment.getAmount(), paymentCode);
            throw new BusinessException(BusinessError.VALIDATION_FAILED);
        }

        // 4. Cập nhật trạng thái tbl_payment -> PAID
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(Instant.now());
        paymentRepository.save(payment);
        log.info("Đã cập nhật trạng thái thanh toán thành công cho mã {}", paymentCode);

        // 5. Mở khóa sách cho khách hàng nếu chưa sở hữu
        if (!customerBookService.hasPurchasedBook(payment.getCustomerId(), payment.getBookId())) {
            customerBookService.assignBookToCustomer(payment.getCustomerId(), payment.getBookId());
        }
    }

    private PaymentResponse buildResponse(Payment payment, Book book) {
        PaymentResponse response = responseMapper.toPaymentResponse(payment, book);
        response.setQrUrl(paymentGateway.createQrUrl(payment));
        return response;
    }

    private String generatePaymentCode() {
        return "SP" + (System.currentTimeMillis() % 10000000L) + String.format("%03d",
            RANDOM.nextInt(1000));
    }
}
