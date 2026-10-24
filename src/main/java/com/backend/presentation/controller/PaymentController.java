package com.backend.presentation.controller;

import com.backend.application.service.PaymentService;
import com.backend.domain.dto.request.payment.CreatePaymentRequest;
import com.backend.domain.dto.response.ApiResponse;
import com.backend.domain.dto.response.payment.PaymentResponse;
import com.backend.infrastructure.persistence.repository.JpaCustomerBookRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final JpaCustomerBookRepository customerBookRepository;

    @PostMapping
    public ApiResponse<PaymentResponse> createPayment(
            @AuthenticationPrincipal Long customerId,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        PaymentResponse response = paymentService.createPayment(customerId, request.getBookId());
        return ApiResponse.<PaymentResponse>builder()
                .message("Tạo hoặc lấy thông tin thanh toán thành công")
                .data(response)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<PaymentResponse> getPaymentStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal Long customerId
    ) {
        PaymentResponse response = paymentService.getPaymentStatus(id, customerId);
        return ApiResponse.<PaymentResponse>builder()
                .message("Lấy trạng thái thanh toán thành công")
                .data(response)
                .build();
    }

    @GetMapping("/pending/{bookId}")
    public ApiResponse<PaymentResponse> getPendingPayment(
            @PathVariable Long bookId,
            @AuthenticationPrincipal Long customerId
    ) {
        PaymentResponse response = paymentService.getPendingPaymentByBook(customerId, bookId);
        return ApiResponse.<PaymentResponse>builder()
                .message(response != null ? "Tìm thấy đơn chờ thanh toán" : "Không có đơn chờ thanh toán")
                .data(response)
                .build();
    }

    @GetMapping("/check/{bookId}")
    public ApiResponse<Map<String, Boolean>> checkPurchase(
            @PathVariable Long bookId,
            @AuthenticationPrincipal Long customerId
    ) {
        boolean purchased = customerId != null && customerBookRepository.existsByCustomerIdAndBookId(customerId, bookId);
        return ApiResponse.<Map<String, Boolean>>builder()
                .message("Kiểm tra trạng thái sở hữu sách")
                .data(Map.of("purchased", purchased))
                .build();
    }
}
